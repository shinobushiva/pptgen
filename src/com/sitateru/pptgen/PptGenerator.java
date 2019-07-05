package com.sitateru.pptgen;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;

import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.PictureData.PictureType;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PptGenerator {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {

		boolean pipe = false;
		if (args.length > 0) {
			pipe = true;
		}

		try {
			String fontFamily = "メイリオ";
			XMLSlideShow ppt = new XMLSlideShow();
			ppt.setPageSize(new Dimension(640, 480));
			XSLFSlideMaster defaultMaster = ppt.getSlideMasters().get(0);
			XSLFSlideLayout layout = defaultMaster.getLayout(SlideLayout.BLANK);

			HashMap<String, Object> map = null;
			if (!pipe) {
				map = new ObjectMapper().readValue(new File("sample.json"), HashMap.class);
			} else {
				map = new ObjectMapper().readValue(new BufferedInputStream(System.in), HashMap.class);
			}

			List<HashMap<String, Object>> pages = (List<HashMap<String, Object>>) map.get("pages");
			pages.forEach(x -> {
				List<HashMap<String, Object>> contents = (List<HashMap<String, Object>>) x.get("contents");

				XSLFSlide slide = ppt.createSlide(layout);
				contents.forEach(content -> {
					try {

						String type = (String) content.get("type");
						if ("text".equals(type)) {
							processTextEntry(fontFamily, slide, content);
						}
						if ("image".equals(type)) {
							processImageEntry(ppt, slide, content);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			});

			BufferedOutputStream bos = new BufferedOutputStream(System.out);
			ppt.write(bos);
			bos.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void processImageEntry(XMLSlideShow ppt, XSLFSlide slide, HashMap<String, Object> content)
			throws IOException, MalformedURLException {
		System.err.println("processImageEntry");
		String url = (String) content.get("url");
		System.err.println(url);
		URLConnection connection = URI.create(url).toURL().openConnection();
		String mimeType = connection.getContentType();
		PictureType pictureType = PictureData.PictureType.valueOf(mimeType.split("/")[1].toUpperCase());
		byte[] pictureData = IOUtils.toByteArray(connection.getInputStream());

		XSLFPictureData pd = ppt.addPicture(pictureData, pictureType);
		XSLFPictureShape picture = slide.createPicture(pd);
		picture.setAnchor(new Rectangle((int) content.get("x"), (int) content.get("y"), (int) content.get("w"),
				(int) content.get("h")));
	}

	private static void processTextEntry(String fontFamily, XSLFSlide slide, HashMap<String, Object> content) {
		System.err.println("processTextEntry");
		XSLFTextBox shape = slide.createTextBox();
		XSLFTextParagraph p = shape.addNewTextParagraph();
		XSLFTextRun r = p.addNewTextRun();
		r.setText((String) content.get("text"));
		r.setFontFamily(fontFamily);
		r.setFontColor(Color.BLACK);
		r.setFontSize((double) content.get("fontSize"));
		shape.setAnchor(new Rectangle((int) content.get("x"), (int) content.get("y"), (int) content.get("w"),
				(int) content.get("h")));
	}

}
