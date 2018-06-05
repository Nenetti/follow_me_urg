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

import org.ros.message.MessageListener;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

public class Follow_Me {

	private boolean setup;
	private double fitDistance=0.75;

	public void start(ConnectedNode connectedNode) {
		final Publisher<geometry_msgs.Twist> cmd_vel = connectedNode.newPublisher("/cmd_vel_mux/input/teleop", geometry_msgs.Twist._TYPE);
		Subscriber<sensor_msgs.LaserScan> subscriberURG=connectedNode.newSubscriber("/scan", sensor_msgs.LaserScan._TYPE);
		subscriberURG.addMessageListener(new MessageListener<sensor_msgs.LaserScan>() {
			@Override
			public void onNewMessage(sensor_msgs.LaserScan message) {
				//アングル幅　+-2.35619rad, +-134.99 = +-135
				//角度増加値 0.006554075rad, 0.37度
				//スキャン最小距離 0.1m
				//スキャン最大距離 30m
				//データサイズ 720
				double[] data=new double[message.getRanges().length];
				double m=message.getAngleIncrement();
				for(int i=0;i<message.getRanges().length;i++) {
					data[i]=message.getRanges()[i];
				}
				int index=getHuman(data, message.getAngleIncrement());
				double angle=m*index+message.getAngleMin();
				double d=data[index];
				
				geometry_msgs.Twist twist=cmd_vel.newMessage();
				if(Math.abs(fitDistance-d)>0.1) {
					if(fitDistance>d) {
						twist.getLinear().setX(-calcVelocityLinear(Math.abs(d), 0.05, 0.1));
					}else {
						twist.getLinear().setX(calcVelocityLinear(Math.abs(d), 0.05, 0.1));
					}
				}
				if(Math.abs(angle)>0.1) {
					if(angle<0) {
						twist.getAngular().setZ(calcVelocityAngle(angle, 0.75, 1.0));
					}else {
						twist.getAngular().setZ(calcVelocityAngle(angle, 0.75, 1.0));
					}
				}
				cmd_vel.publish(twist);
				if(setup) {
					setup=false;
				}
			}
		});
	}
	private double calcVelocityLinear(double d, double min, double max) {
		if(Math.abs(d)<0.01) {
			return 0;
		}
		if(d>max) {
			return max;
		}else {
			if(d>min){
				return d;
			}else {
				return min;
			}
		}
	}
	
	private double calcVelocityAngle(double d, double min, double max) {
		if(Math.abs(d)<0.01) {
			return 0;
		}
		if(d>0) {
			if(d>max) {
				return max;
			}else {
				if(d>min) {
					return d;
				}else {
					return min;
				}
			}
		}else {
			if(d<-max) {
				return -max;
			}else {
				if(d<-min) {
					return d;
				}else {
					return -min;
				}
			}
		}
	}

	public int getHuman(double[] data, double angleIncrement) {
		//配列260〜460程度を正面と定義
		//真正面は配列360番
		
		int center=data.length/2;
		int index=data.length/2;
		double min=Integer.MAX_VALUE;
		for(int i=0;i<100;i++) {
			double dl=data[center+i];
			if(dl>0.4) {
				if(min>dl) {
					min=dl;
					index=center+i;
				}
			}
			double dr=data[center-i];
			if(dr>0.4) {
				if(min>dr) {
					min=dr;
					index=center-i;
				}
			}
		}
		return index;
	}
}
