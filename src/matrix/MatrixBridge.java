package matrix;

import arc.*;
import arc.graphics.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.gen.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;

import java.nio.file.Files;
import java.nio.file.Path;

public class MatrixBridge extends Plugin {
	private Color colour;

	@Override
	public void init() {
		try {
			var token = readLine("matrix_token");
			var room = readLine("matrix_room");
			var server = readLine("matrix_server");

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
			String.format("{\"body\":\"%s: %s\",\"msgtype\":\"m.text\"}", event.player.name, event.message);
			Http.post(url, content)
				.submit(res -> {});
		});

		} catch (Exception e) {}
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
				case 0:
					opened--;
					sb.append("</font>");
					i++;
					break;
				case -1:
					sb.append('[');
					break;
				case -2:
					i++;
					break;
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
}
