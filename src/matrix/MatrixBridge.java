package matrix;

import arc.*;
import arc.graphics.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.gen.*;
import mindustry.game.EventType.*;
import mindustry.io.*;
import mindustry.mod.*;

import java.nio.file.Files;
import java.nio.file.Path;

public class MatrixBridge extends Plugin {
	private String token, room, server, self;
	private Color colour;
	private String to = "";
	private StringMap names = new StringMap();

	@Override
	public void init() {
		token = readLine("matrix_token");
		room = readLine("matrix_room");
		server = readLine("matrix_server");
		self = readLine("matrix_user");

		// Bridge mindustry -> matrix
		Events.on(PlayerChatEvent.class, event -> {
			var url = String.format("%s/_matrix/client/r0/rooms/%s/send/m.room.message?access_token=%s", server, room, token);
			// TODO: thing like @_mindustry_name:anuke.eu.org
			var body = String.format("[#ffd37f]%s[]: %s", event.player.name, event.message);
			var value = new JsonValue(JsonValue.ValueType.object);
			value.addChild("msgtype", new JsonValue("m.text"));
			value.addChild("format", new JsonValue("org.matrix.custom.html"));
			value.addChild("body", new JsonValue(Strings.stripColors(body)));
			value.addChild("formatted_body", new JsonValue(render(body)));
			var content = value.toJson(JsonWriter.OutputType.json);
			Http.post(url, content)
				.submit(res -> {});
		});

		// Bridge matrix -> mindustry
		var gaming = new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					Log.err(e);
				}

				var url = String.format("%s/_matrix/client/v3/rooms/%s/messages?access_token=%s&dir=b%s", server, room, token, to);

				Http.get(url, res -> {
					var jason = Jval.read(res.getResultAsString());
					to = "&to=" + jason.getString("start");
					Log.info("@", jason.get("chunk").asArray().size);
					jason.get("chunk").asArray().each(event -> {
						if (room.equals(event.getString("room_id"))) {
							var sender = event.getString("sender");
							if (sender != self) {
								Call.sendMessage(String.format("[coral]<[]%s[coral]>[]: %s",
									getDisplayName(sender),
									event.get("content").getString("body")));
							}
						}
					});
				});
			}
		}, "Bridge thread");
		gaming.setDaemon(true);
		gaming.start();
	}

	private String readLine(String name) {
		try {
			return Files.readString(Path.of(name)).trim();
		} catch (Exception e) {
			Log.err(e);
			return "";
		}
	}

	// escape html tags and render colours
	private String render(String raw) {
		var sb = new StringBuilder();
		int opened = 0;
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			switch (c) {
			// html escaping
			case '<':
				sb.append("&lt;");
				break;
			case '>':
				sb.append("&gt;");
				break;
			case '&':
				sb.append("&amp;");
				break;
			case '\'':
				sb.append("&apos;");
				break;
			case '"':
				sb.append("&quot;");
				break;

			// colour rendering
			case '[':
				int len = parseMarkup(raw, i + 1);
				switch (len) {
				// escaped
				case -2:
					sb.append('[');
					i++;
					break;
				// ignore
				case -1:
					sb.append('[');
					break;
				// pop colour
				case 0:
					opened--;
					sb.append("</font>");
					i++;
					break;
				// push colour
				default:
					i += len + 1;
					opened++;
					sb.append("<font color=\"#");
					var str = colour.toString();
					// ignore opacity
					sb.append(str.substring(0, 6));
					sb.append("\">");
					break;
				}
				break;

			default:
				sb.append(c);
				break;
			}
		}

		// close unclosed colours
		for (int i = 0; i < opened; i++) {
			sb.append("</font>");
		}

		return sb.toString();
	}

	private int parseMarkup(String str, int start) {
		int end = str.length();
		if (start == end) return -1;

		switch (str.charAt(start)) {
		case '#':
			// Parse hex color RRGGBBAA where AA is optional and defaults to 0xFF if less than 6 chars are used.
			for (int i = start + 1; i < end; i++) {
				char c = str.charAt(i);
				if (c == ']') {
					if (i < start + 2 || i > start + 9) break; // Ilegal number of hex digits
					colour = Color.valueOf(str.substring(start + 1, i));
					return (colour == null) ? -1 : i - start;
				}
			}

			return -1;
		case '[':
			return -2;
		case ']':
			return 0;
		}

		for (int i = start + 1; i < end; i++) {
			char ch = str.charAt(i);
			if (ch != ']') continue;

			colour = Colors.get(str.substring(start, i));
			return (colour == null) ? -1 : i - start;
		}

		return -1; // unclosed colour tag
	}

	private String getDisplayName(String id) {
		var known = names.get(id);
		if (known != null) return known;

		var url = String.format("%s/_matrix/v3/profile/%s?access_token=%s", server, id, token);
		Http.get(url, res -> {
			var jason = Jval.read(res.getResultAsString());
			var name = jason.getString("displayname");
			names.put(id, (name != null) ? name : id);
		});

		// return id until display name is known
		return id;
	}
}
