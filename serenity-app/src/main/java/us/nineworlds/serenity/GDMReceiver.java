package us.nineworlds.serenity;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.Map;
import javax.inject.Inject;
import us.nineworlds.serenity.core.model.Server;
import us.nineworlds.serenity.core.model.impl.GDMServer;
import us.nineworlds.serenity.core.services.GDMService;
import us.nineworlds.serenity.injection.ForMediaServers;
import us.nineworlds.serenity.injection.InjectingBroadcastReceiver;
import us.nineworlds.serenity.injection.SerenityObjectGraph;

public class GDMReceiver extends InjectingBroadcastReceiver {

	SerenityObjectGraph objectGraph;

	@Inject
	@ForMediaServers
	Map<String, Server> servers;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (objectGraph == null) {
			objectGraph = SerenityObjectGraph.getInstance();
			objectGraph.inject(this);
		}

		if (intent.getAction().equals(GDMService.MSG_RECEIVED)) {
			String message = intent.getStringExtra("data").trim();
			String ipAddress = intent.getStringExtra("ipaddress").substring(1);
			Server server = new GDMServer();

			int namePos = message.indexOf("Name: ");
			namePos += 6;
			int crPos = message.indexOf("\r", namePos);
			String serverName = message.substring(namePos, crPos);

			server.setServerName(serverName);
			server.setIPAddress(ipAddress);
			if (!servers.containsKey(serverName)) {
				servers.put(serverName, server);
				Log.d(getClass().getName(), "Adding " + serverName);
			} else {
				Log.d(getClass().getName(), serverName + " already added.");
			}
		} else if (intent.getAction().equals(GDMService.SOCKET_CLOSED)) {
			Log.i("GDMService", "Finished Searching");
		}
	}
}
