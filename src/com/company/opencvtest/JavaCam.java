package com.company.opencvtest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

public class JavaCam {
	public static final boolean WRITE = true;
	public static final int FRAMES = 100;
	
	private VideoCapture vc = null;
	private Boolean bExit = false;

	
	private Thread t;
	private String ip;
	private String mac;
	public JavaCam(String mac, String ip) {
		this.mac = mac;
		this.ip = ip;
	}
	public int init() {
		return 0;
	}

	public int start() {
		t = new Thread(new Runnable() {
			@Override
			public void run() {
				String rtsp = "rtsp://admin:Marvell12@" + ip + ":554/ch0/main/av_stream";
				System.out.println("Launch camera thread :" + rtsp);
				vc = new VideoCapture(rtsp);
				String threadName = Thread.currentThread().getName();
				try {
					Runtime.getRuntime().exec(new String[] { "sh", "-c", String.format("mkdir -p /tmp/%s", threadName) });
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				while (!bExit) {
					if (vc.grab()) {

						Mat frame = new Mat();
						MatOfByte mem = new MatOfByte();

						// Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", String.format("cd
						// /home/lab/tmp && rm -rf %s", threadName) });
						// Runtime.getRuntime().exec(new String[] { "sh", "-c", String.format("cd /tmp
						// && mkdir -p %s", threadName) });

						long start = System.currentTimeMillis();
						for (int i = 0; i < FRAMES; ++i) {
							try {
							vc.read(frame);
								if (!WRITE) {
									System.out.println(threadName + String.format(": read %d-th frame", i));
									Highgui.imencode(".jpg", frame, mem);
									
									FramePkg pkg = new FramePkg(frame.width(), frame.height(), mem.toArray(), "jpg", mac);
									Dispatch.dispatch(pkg);

								}
								if (WRITE) {
									Highgui.imencode(".jpg", frame, mem);
									Image im = ImageIO.read(new ByteArrayInputStream(mem.toArray()));
									BufferedImage buff = (BufferedImage) im;
	
									String path = "/tmp/" + threadName + String.format("/image_%04d.jpg", i);
									ImageIO.write(buff, "jpg", new File(path));
									System.out.println(threadName + String.format(": write %d-th frame", i));
								}
							} catch (Exception e) {
								// TODO: handle exception
								e.printStackTrace();
							}
							long end = System.currentTimeMillis();

							System.out.printf("**** the FPS of %s is %d\n\n", threadName, FRAMES * 1000 / (end - start));
						}
					}
				}
			}
		}, ip); // use ip as thread name
		t.start();
		return 0;
	}
	static public class Tuple<X, Y> { 
	    public final X x; 
	    public final Y y; 
	    public Tuple(X x, Y y) { 
	        this.x = x; 
	        this.y = y; 
	    }

	    @Override
	    public String toString() {
	        return "(" + x + "," + y + ")";
	    }

	    @Override
	    public boolean equals(Object other) {
	        if (other == this) {
	            return true;
	        }

	        if (!(other instanceof Tuple)){
	            return false;
	        }

	        Tuple<X,Y> other_ = (Tuple<X,Y>) other;

	        // this may cause NPE if nulls are valid values for x or y. The logic may be improved to handle nulls properly, if needed.
	        return other_.x.equals(this.x) && other_.y.equals(this.y);
	    }

	    @Override
	    public int hashCode() {
	        final int prime = 31;
	        int result = 1;
	        result = prime * result + ((x == null) ? 0 : x.hashCode());
	        result = prime * result + ((y == null) ? 0 : y.hashCode());
	        return result;
	    }
	}
	
	
	static public List<String> ReadCmdLine(String[] cmd) {  
	        Process process = null;  
	        List<String> processList = new ArrayList<String>();  
	        try {  
	            process = Runtime.getRuntime().exec(cmd);  
	            int exitValue = process.waitFor();  
	            if (0 != exitValue) {  
	            	System.out.println("call shell failed. error code is :" + exitValue);  
	            }
	        
	            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));  
	            String line = "";  
	            while ((line = input.readLine()) != null) {  
	                processList.add(line);  
	            }  
	            input.close();  
	        } catch (IOException e) {  
	            e.printStackTrace();  
	        } catch (Throwable e) {  
	        	System.out.println("call shell failed. " + e);  
	        }  
	  
	        for (String line : processList) {  
	            System.out.println(line);  
	        }
	        return processList;
	}  
	
	
	public static void main(String[] args) throws IOException {
		System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
		Vector<String> mac_table = new Vector<String>();
	    mac_table.add("b4:a3:82:5e:5f:15");
	    mac_table.add("4c:bd:8f:c6:30:1f");
	    String IPDomain = "192.168.10.0/24";

		
		Vector<Tuple<String, String>> mac_ip_table = new Vector<Tuple<String, String>>();

		System.out.println("Start to scan local device ip-mac tables.....");
		String[] cmd = {
				"/bin/sh",
				"-c",
				"sudo nmap -sP  " + IPDomain + " | awk '/Nmap scan report for/{printf $5;}/MAC Address:/{print \" => \"$3;}' | sort"
				//"sudo nmap -sP  192.168.10.0/24 | awk '/Nmap scan report for/{printf $5;}/MAC Address:/{print \" => \"$3;}' | sort"
				};
		List<String> res = ReadCmdLine(cmd);
		if (res == null) {
			System.out.println("fail to execute arp");
			return;
		}
		
		for (String line:res) {
			line = line.toLowerCase();
			for (String mac:mac_table){
				mac=mac.toLowerCase();
				String regEx = "([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+).*=>.*" + mac + ".*";
			
				Pattern pattern = Pattern.compile(regEx);
				Matcher m = pattern.matcher(line);
				if (m.find()) {
					System.out.println("Found value: " + m.group(0) );
					String ip = m.group(1);
					mac_ip_table.add(new Tuple<String, String>(mac, ip) );
					break; //search next line
				}
			}
		}
		
		for (Tuple<String, String> s: mac_ip_table) {
			String mac = s.x;
			String ip = s.y;
			JavaCam cam = new JavaCam(mac, ip);
			cam.start();
		}
		
		System.out.println("done");
		
	}
}









































