/*
 * Copyright (C) 2014 kanechika.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package follow_me;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;





public class Main extends AbstractNodeMain {

	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of("rosjava/navigation/follow_me");
	}

	@Override
	public void onStart(ConnectedNode connectedNode) {
		
	}
	
	public void receivedMessage(final ConnectedNode connectedNode) {
		Subscriber<std_msgs.String> start_subscriber=connectedNode.newSubscriber("follow_me", std_msgs.String._TYPE);
		start_subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
			@Override
			public void onNewMessage(std_msgs.String message) {
				switch(message.getData()) {
				case "start":
					Follow_Me follow_Me=new Follow_Me();
					follow_Me.start(connectedNode);
					break;
				case "stop":
					System.exit(0);
					break;
				}
			}
		});
		
		
	}
	
}