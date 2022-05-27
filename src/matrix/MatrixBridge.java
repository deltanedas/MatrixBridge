package matrix;

import mindustry.gen.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;

public class MatrixBridge extends Plugin {
	//called when game initializes
	@Override
	public void init() {
		// Listen for mindustry messages
		Events.on(PlayerChatEvent.class, event -> {
			Call.sendMessage(String.format("[stat]%s[]: %s", event.player.name, event.message));
		});
	}
}
