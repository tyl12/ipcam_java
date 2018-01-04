package com.company.opencvtest;

import java.awt.image.BufferedImage;

public class FramePkg {
	int width = -1;
	int height = -1;
	byte[] img;
	long ts = -1;
	String fmt = "jpg";
	String camId = "NA";
	
	
	public FramePkg(int width, int height, byte[] buf, String fmt, String camId){
		this.width = width;
		this.height = height;
		this.ts = System.currentTimeMillis();
		this.fmt = fmt;
		this.camId = camId;
		this.img = buf;
	}
}
