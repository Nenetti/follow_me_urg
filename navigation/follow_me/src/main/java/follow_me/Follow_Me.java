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

	private final double fitDistance=0.75;
	private final double VelocityLinearMax=0.1;
	private final double VelocityLinearMin=0.05;
	private final double VelocityAngularMax=1.5;
	private final double VelocityAngularMin=1.0;
	
	/******************************************************************************
	 * 
	 * Subscriber,Publisherの定義
	 * 
	 * @param connectedNode
	 */
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
				if(Math.abs(d-fitDistance)>0.1) {
					twist.getLinear().setX(calcVelocityLinear(d-fitDistance, VelocityLinearMin, VelocityLinearMax));
				}
				if(Math.abs(angle)>0.1) {
					twist.getAngular().setZ(calcVelocityAngular(angle, VelocityAngularMin, VelocityAngularMax));
				}
				//タートルボットに速度を送信
				cmd_vel.publish(twist);
			}
		});
	}

	/******************************************************************************
	 * 
	 * 直進速度を計算する
	 * 
	 * @param d		距離
	 * @param min	最小速度
	 * @param max	最大速度
	 * @return		速度
	 */
	private double calcVelocityLinear(double d, double min, double max) {
		//絶対値距離と距離による速度方向を設定
		double distance=Math.abs(d);
		if(distance<0.01) {
			return 0;
		}
		int variable=1;
		if(d<0) {
			variable=-1;
		}
		//指定距離Maxより遠ければ速度を制限
		//指定距離Minより近ければ速度を制限
		//指定距離Max以下,指定距離Min以内ならそのまま値を使用
		if(distance>max) {
			return max*variable;
		}else {
			if(distance>min){
				return distance*variable;
			}else {
				return min*variable;
			}
		}	
	}

	/******************************************************************************
	 * 
	 * 回転速度を計算する
	 * 
	 * @param d		角度差
	 * @param min	最小速度
	 * @param max	最大速度
	 * @return		速度
	 */
	private double calcVelocityAngular(double d, double min, double max) {
		//絶対値角度差よる速度方向を設定
		double angle=Math.abs(d);
		if(angle<0.01) {
			return 0;
		}
		int variable=1;
		if(d<0) {
			variable=-1;
		}
		//指定距離Maxより大きければ速度を制限
		//指定距離Minより小さければ速度を制限
		//指定距離Max以下,指定距離Min以内ならそのまま値を使用
		if(d>max) {
			return max*variable;
		}else {
			if(d>min) {
				return angle*variable;
			}else {
				return min*variable;
			}
		}
	}

	/******************************************************************************
	 * 
	 * URGデータから人がいるであろうインデックスを検索する
	 * 
	 * @param data				URGデータ
	 * @param angleIncrement	URGデータの1インデックスごとの角度増加量
	 * @return					人と思われるインデックス
	 */
	public int getHuman(double[] data, double angleIncrement) {
		//配列260〜460程度を正面と定義
		//真正面は配列360番
		//正面から角度がずれるほど距離に補正をかける
		//正面方向における最小距離のインデックスを探す
		int center=data.length/2;
		int index=data.length/2;
		double variable=0.005;
		double min=Integer.MAX_VALUE;
		for(int i=0;i<125;i++) {
			double dl=data[center+i]+variable*i;
			if(dl>0.4) {
				if(min>dl) {
					min=dl;
					index=center+i;
				}
			}
			double dr=data[center-i]+variable*i;
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
