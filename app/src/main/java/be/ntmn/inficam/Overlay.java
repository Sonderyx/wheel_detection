package be.ntmn.inficam;

import static java.lang.Float.NaN;
import static java.lang.Float.isNaN;
import static java.lang.Math.max;
import static java.lang.Math.min;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.appcompat.content.res.AppCompatResources;

import be.ntmn.libinficam.InfiCam;

public class Overlay {
	public static class Data {
		public InfiCam.FrameInfo fi = new InfiCam.FrameInfo();
		public float[] temp;
		public int[] palette;
		public float rangeMin = NaN, rangeMax = NaN;
		public boolean rotate = false, mirror = false, rotate90 = false; /* Set by Settings. */
		public boolean showMin = false; /* Set by SettingsTherm. */
		public boolean showMax = false;
		public boolean showAvg = false;
		public boolean showCenter = false;
		public boolean centralDisplay = false;
		public boolean showPalette = false;
		public float scale = 1.0f;
		public int tempUnit = Util.TEMPUNIT_CELSIUS;
		public int noDigits = 1; // 0-3 decimal places
		public int iCentralValidation;
		public int iCentralTheight;
		public int iCentralCaptionTextWidth;
		public int iCentralTemperatureTextWidth;
		public int iCentralCaptionLeft;
		public int iCentralTemperatureLeft;
	}

	public final SurfaceMuxer.InputSurface surface;
	private final Paint paint;
	private final Paint paintOutline;
	private final Paint paintTextOutline;
	private final Paint paintPalette;
	private final Drawable lock;
	private int width, height;
	private final Rect vRect = new Rect(), rectTgt = new Rect(); /* Do not alloc each frame! */
	private int[] paletteCache_palette;
	private final int[] getPaletteCache_array = new int[InfiCam.paletteLen];
	private final Bitmap paletteCache_bitmap =
			Bitmap.createBitmap(1, InfiCam.paletteLen, Bitmap.Config.ARGB_8888);
	private final Bitmap paletteCacheH_bitmap =
			Bitmap.createBitmap(InfiCam.paletteLen, 1, Bitmap.Config.ARGB_8888);

	/* These sizes are in fractions of the total width of the bitmap drawn. */
	private final static float smarker = 0.030f; /* Marker size. */
	private final static float smarkerhole = 0.010f; /* Marker hole size. */
	private final static  float wmarker = 0.003f; /* How fat the markers are. */
	private final static float toff = 0.015f; /* How far to put the text away from marker. */
	private final static float tclearance = 0.005f; /* How far the text should stay from edges. */
	private final static float textsize = 0.035f;
	private final static float woutline = 0.008f; /* Text outline thickness. */
	private final static float pwidth = 0.038f; /* Palette preview width. */
	private final static float pclearance = 0.016f;

	private final StringBuilder sb = new StringBuilder();

	public static class MinMaxAvg {
		float min, max, avg;
		int min_x, min_y, max_x, max_y;
	}

	public Overlay(Context ctx, SurfaceMuxer.InputSurface is) {
		surface = is;
		paint = new Paint();
		paintPalette = new Paint();
		paintPalette.setAntiAlias(false);
		paint.setAntiAlias(true);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paintOutline = new Paint(paint);
		paintOutline.setStyle(Paint.Style.STROKE);
		paintTextOutline = new Paint(paint);
		paintTextOutline.setColor(Color.BLACK);
		paintTextOutline.setStyle(Paint.Style.STROKE);
		lock = AppCompatResources.getDrawable(ctx, R.drawable.ic_baseline_lock_24_2);
	}

	public void setSize(int w, int h) {
		width = w;
		height = h;
		surface.setSize(w, h);
	}

	public static void mmaRect(MinMaxAvg out, float[] temp, int left, int top,
							   int right, int bottom, int stride) {
		out.min = out.max = NaN;
		out.avg = 0.0f;
		out.min_x = out.min_y = out.max_x = out.max_y = 0;
		for (int y = top; y < bottom; ++y) {
			for (int x = left; x < right; ++x) {
				float t = temp[y * stride + x];
				if (t < out.min || isNaN(out.min)) {
					out.min = t;
					out.min_x = x;
					out.min_y = y;
				}
				if (t > out.max || isNaN(out.max)) {
					out.max = t;
					out.max_x = x;
					out.max_y = y;
				}
				out.avg += t;
			}
		}
		out.avg /= (right - left) * (bottom - top);
	}

	private void drawText(Canvas cvs, StringBuilder sb, float x, float y, boolean la, boolean ta) {
		float theight = (int) -(paint.descent() + paint.ascent());
		paint.setTextAlign(la ? Paint.Align.LEFT : Paint.Align.RIGHT);
		paintTextOutline.setTextAlign(la ? Paint.Align.LEFT : Paint.Align.RIGHT);
		cvs.drawText(sb, 0, sb.length(), x, y + (ta ? theight : 0), paintTextOutline);
		cvs.drawText(sb, 0, sb.length(), x, y + (ta ? theight : 0), paint);
	}

	@SuppressLint("DefaultLocale")
	public void draw(Data d, Rect rect) {
		Canvas cvs = surface.surface.lockCanvas(null);
		cvs.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

		int w = rect.width();
		vRect.set(rect);
		paint.setStrokeWidth(wmarker * w);
		paint.setTextSize(textsize * w);
		paintOutline.setStrokeWidth(wmarker * w * 3);
		paintTextOutline.setStrokeWidth(woutline * w);
		paintTextOutline.setTextSize(textsize * w);

		//if (d.centralDisplay)
			drawCentralMeasureVal(cvs, d);

		if (d.showCenter) { // TODO this is off by a pixel and we should check the other points too
			paint.setColor(Color.rgb(255, 255, 0)); // Yellow.
			drawTPoint(cvs, d, d.fi.width / 2, d.fi.height / 2, d.fi.center);
		}

		if (d.showMin) {
			paint.setColor(Color.rgb(0, 127, 255)); // Blue.
			drawTPoint(cvs, d, d.fi.min_x, d.fi.min_y, d.fi.min);
		}

		if (d.showMax) {
			paint.setColor(Color.rgb(255, 64, 64)); // Red.
			drawTPoint(cvs, d, d.fi.max_x, d.fi.max_y, d.fi.max);
		}
		/*
		if (d.showAvg) {
			paint.setColor(Color.rgb(200, 200, 200)); // light grey
			if (!d.centralDisplay) {
				Util.formatTemp(sb, d.fi.avg, d.tempUnit);
				sb.insert(0,"Avg ");
				if (width <= vRect.width())
					drawText(cvs, sb, vRect.left, vRect.bottom - 10, true, false);
				else
					drawText(cvs, sb, vRect.left / 2f, vRect.bottom - 10, true, false);
			}
		}*/

		if (d.showPalette) {
			int clear = (int) (pclearance * vRect.width());
			int theight = (int) -(paint.descent() + paint.ascent());
			int isize = (int) (theight + woutline * vRect.width());
			int iclear = (int) (clear - (woutline * vRect.width()) / 2.0f);

			if(width > height) { // landscape on the right side
				paint.setColor(Color.WHITE);
				if (width <= vRect.width()) {
					drawPalette(cvs,
							(int) (vRect.right - clear - pwidth * vRect.width()),
							vRect.top + theight + clear * 2,
							vRect.right - clear,
							vRect.bottom - theight - clear * 2,
							d.palette);
					Util.formatTemp(sb, Float.isNaN(d.rangeMax) ? d.fi.max : d.rangeMax, d.tempUnit, d.noDigits);
					drawText(cvs, sb, vRect.right - clear, vRect.top + clear, false, true);
					if (!Float.isNaN(d.rangeMax)) {
						int off = (int) paintTextOutline.measureText(sb, 0, sb.length());
						lock.setBounds(vRect.right - clear - off - isize, vRect.top + iclear,
								vRect.right - clear - off, vRect.top + iclear + isize);
						lock.draw(cvs);
					}
					Util.formatTemp(sb, Float.isNaN(d.rangeMin) ? d.fi.min : d.rangeMin, d.tempUnit, d.noDigits);
					drawText(cvs, sb, vRect.right - clear, vRect.bottom - clear, false, false);
					if (!Float.isNaN(d.rangeMin)) {
						int off = (int) paintTextOutline.measureText(sb, 0, sb.length());
						lock.setBounds(vRect.right - clear - off - isize, vRect.bottom - iclear - isize,
								vRect.right - clear - off, vRect.bottom - iclear);
						lock.draw(cvs);
					}
				} else {
					drawPalette(cvs,
							vRect.right + clear,
							vRect.top + theight + clear * 2,
							(int) (vRect.right + clear + pwidth * vRect.width()),
							vRect.bottom - theight - clear * 2,
							d.palette);
					Util.formatTemp(sb, Float.isNaN(d.rangeMax) ? d.fi.max : d.rangeMax, d.tempUnit, d.noDigits);
					drawText(cvs, sb, vRect.right + clear, vRect.top + clear, true, true);
					if (!Float.isNaN(d.rangeMax)) {
						int off = (int) paintTextOutline.measureText(sb, 0, sb.length());
						lock.setBounds(vRect.right + clear + off, vRect.top + iclear,
								vRect.right + clear + off + isize, vRect.top + iclear + isize);
						lock.draw(cvs);
					}
					Util.formatTemp(sb, Float.isNaN(d.rangeMin) ? d.fi.min : d.rangeMin, d.tempUnit, d.noDigits);
					drawText(cvs, sb, vRect.right + clear, vRect.bottom - clear, true, false);
					if (!Float.isNaN(d.rangeMin)) {
						int off = (int) paintTextOutline.measureText(sb, 0, sb.length());
						lock.setBounds(vRect.right + clear + off, vRect.bottom - iclear - isize,
								vRect.right + clear + off + isize, vRect.bottom - iclear);
						lock.draw(cvs);
					}
				}
			} else { // portrait mode, bottom of the ir-image
				drawPalette(cvs,
						vRect.left + vRect.width() / 6,
						vRect.bottom + clear,
						vRect.right - vRect.width() / 6,
						(int)(vRect.bottom + clear + pwidth * vRect.height()),
						d.palette);
				paint.setColor(Color.WHITE);
				Util.formatTemp(sb, Float.isNaN(d.rangeMax) ? d.fi.max : d.rangeMax, d.tempUnit, d.noDigits);
				drawText(cvs, sb, clear,vRect.bottom + theight + 2 * clear, true, false, 1);
				if (!Float.isNaN(d.rangeMax)) {
					int off = (int) paintTextOutline.measureText(sb, 0, sb.length());
					lock.setBounds(vRect.left + clear + off, vRect.bottom + iclear,
							vRect.left + clear + off + isize, vRect.bottom + iclear + isize);
					lock.draw(cvs);
				}
				Util.formatTemp(sb, Float.isNaN(d.rangeMin) ? d.fi.min : d.rangeMin, d.tempUnit, d.noDigits);
				drawText(cvs, sb, vRect.right - clear, vRect.bottom + theight + 2 * clear, false, false, 1);
				if (!Float.isNaN(d.rangeMin)) {
					int off = (int) paintTextOutline.measureText(sb, 0, sb.length());
					lock.setBounds(vRect.right - clear - off - isize, vRect.bottom + iclear,
							vRect.right - clear - off, vRect.bottom + iclear + isize);
					lock.draw(cvs);
				}
			}
		}

		surface.surface.unlockCanvasAndPost(cvs);
	}

	private void drawText(Canvas cvs, StringBuilder sb, float x, float y, boolean la, boolean ta, double fSize) {
		float fTextSize = paint.getTextSize();
		//paint.setTextSize(paint.getTextSize() * (float)fSize);
		//paint.setTextSize(textsize * cvs.getHeight());
		paint.setTextSize(textsize * (float)(fSize * (cvs.getHeight()+cvs.getWidth())/2.5));
		paintTextOutline.setTextSize(paint.getTextSize());
		drawText(cvs, sb, x, y, la, ta);
		paint.setTextSize(fTextSize);
		paintTextOutline.setTextSize(fTextSize);
	}

	public int getTextWidth(Paint paint, String str) { //calculate text width
		int iRet = 0;
		if (str != null && str.length() > 0) {
			int len = str.length();
			float[] widths = new float[len];
			paint.getTextWidths(str, widths);
			for (int j = 0; j < len; j++) {
				iRet += (int) Math.ceil(widths[j]);
			}
		}
		return iRet;
	}

	private void drawSingleMeasureVal(Canvas cvs, Data d, String sCaption, float fMeasureVal, int iPos) {
		fMeasureVal = Util.convertTemperature(fMeasureVal, d.tempUnit);
		sb.setLength(0);
		sb.insert(0, sCaption);
		drawText(cvs, sb, d.iCentralCaptionLeft, vRect.top + (int) (iPos * d.iCentralTheight), true, false, 0.7);
		sb.setLength(0);
		sb.insert(0, String.format(new String("%3." + d.noDigits + "f"), fMeasureVal));
		drawText(cvs, sb, d.iCentralTemperatureLeft, vRect.top + (int) (iPos * d.iCentralTheight), true, false,1.1);
	}

	private void drawCentralMeasureVal(Canvas cvs, Data d) {
		int iPos = 0;

		if (d.iCentralValidation != d.noDigits + vRect.width()) { // calculate text width if number of digits or oriontation is changed
			d.iCentralValidation = d.noDigits + vRect.width();
			float fTextSize = paint.getTextSize();
			paint.setTextSize(textsize * (float)((cvs.getHeight() + cvs.getWidth()) /2.5));
			d.iCentralTheight = (int) -(paint.descent() + paint.ascent()) + (int) (pclearance * vRect.width() * 1.1);
			d.iCentralCaptionTextWidth = getTextWidth(paint, "spot");
			d.iCentralTemperatureTextWidth = getTextWidth(paint, "0") * (4 + d.noDigits);
			d.iCentralCaptionLeft = vRect.left + (int) (pclearance * vRect.width());
			d.iCentralTemperatureLeft = d.iCentralCaptionLeft + d.iCentralCaptionTextWidth;
			paint.setTextSize(fTextSize);
		}

		paint.setColor(Color.rgb(220, 220, 220)); // light grey

		if (d.showCenter && d.centralDisplay)
			drawSingleMeasureVal(cvs, d, "spot", d.fi.center, ++iPos);
		if (d.showMax && d.centralDisplay)
			drawSingleMeasureVal(cvs, d, "max", d.fi.max, ++iPos);
		if (d.showMin && d.centralDisplay)
			drawSingleMeasureVal(cvs, d, "min", d.fi.min, ++iPos);
		if (d.showAvg)
			drawSingleMeasureVal(cvs, d, "avg", d.fi.avg, ++iPos);

		if (iPos > 0) { // at least one measurement to display the unit
			//paint.getTextBounds("100.000", 0, 4+d.noDigits, bounds);
			Util.formatTemp(sb, 0, d.tempUnit, 0);
			if (sb.length() > 3) sb.delete(0, sb.length() - 2);
			//drawText(cvs, sb, iLeftValue + bounds.width(), vRect.top + theight, true, false, 1);
			drawText(cvs, sb, d.iCentralTemperatureLeft + d.iCentralTemperatureTextWidth, vRect.top + d.iCentralTheight, true, false, 1.1);
		}
	}

	private void drawPalette(Canvas cvs, int x1, int y1, int x2, int y2, int[] palette) {
		if (y2 - y1 <= 0)
			return;
		if (paletteCache_palette != palette) {
			for (int i = 0; i < palette.length; ++i)
				getPaletteCache_array[i] =
						Integer.reverseBytes(palette[palette.length - i - 1]) >> 8 | 0xFF000000;
			paletteCache_bitmap.setPixels(getPaletteCache_array, 0, 1, 0, 0,  1, palette.length);
			paletteCacheH_bitmap.setPixels(getPaletteCache_array, 0, palette.length, 0, 0,  palette.length,1);
			paletteCache_palette = palette;
		}
		cvs.drawRect(x1, y1, x2, y2, paintOutline);
		rectTgt.set(x1, y1, x2, y2);
		/* We use the paintPalette for the bitmap to make doubly sure antialias is off, having it
		 *   on causes our 1px line to go transparent.
		 */
		if(width < height) // in portrait mode horizontal palette
			cvs.drawBitmap(paletteCacheH_bitmap, null, rectTgt, paintPalette);
		else // in landscape mode vertical palette
			cvs.drawBitmap(paletteCache_bitmap, null, rectTgt, paintPalette);
	}

	private void drawTPoint(Canvas cvs, Data d, int tx, int ty, float temp) {
		if (d.rotate90) {
			int tmp = tx;
			tx = d.fi.height - ty - 1;
			ty = tmp;
		}

		float xm = (tx + 0.5f) * vRect.width() / (d.rotate90 ? d.fi.height : d.fi.width) * d.scale;
		if (d.rotate)
			xm = vRect.width() * d.scale - xm;
		if (d.mirror)
			xm = vRect.width() * d.scale - xm;
		xm += vRect.left;
		xm -= (vRect.width() * d.scale - vRect.width()) / 2.0f;

		float ym = (ty + 0.5f) * vRect.height() / (d.rotate90 ? d.fi.width : d.fi.height) * d.scale;
		if (d.rotate)
			ym = vRect.height() * d.scale - ym;
		ym += vRect.top;
		ym -= (vRect.height() * d.scale - vRect.height()) / 2.0f;

		/*float smarkerw = smarker * vRect.width();
		cvs.drawLine(xm - smarkerw, ym, xm + smarkerw, ym, paintOutline);
		cvs.drawLine(xm, ym - smarkerw, xm, ym + smarkerw, paintOutline);
		cvs.drawLine(xm - smarkerw, ym, xm + smarkerw, ym, paint);
		cvs.drawLine(xm, ym - smarkerw, xm, ym + smarkerw, paint);*/

		float smarkerw = smarker * vRect.width();
		float smarkerholew = smarkerhole * vRect.width();
		cvs.drawLine(xm - smarkerw, ym, xm - smarkerholew, ym, paintOutline);
		cvs.drawLine(xm + smarkerw, ym, xm + smarkerholew, ym, paintOutline);
		cvs.drawLine(xm, ym - smarkerw, xm, ym - smarkerholew, paintOutline);
		cvs.drawLine(xm, ym + smarkerw, xm, ym + smarkerholew, paintOutline);
		cvs.drawLine(xm - smarkerw, ym, xm - smarkerholew, ym, paint);
		cvs.drawLine(xm + smarkerw, ym, xm + smarkerholew, ym, paint);
		cvs.drawLine(xm, ym - smarkerw, xm, ym - smarkerholew, paint);
		cvs.drawLine(xm, ym + smarkerw, xm, ym + smarkerholew, paint);

		if(!d.centralDisplay) {
			float offX = toff * vRect.width();
			float offY = offX ; //-(paint.descent() + paint.ascent()) / 2.0f;
			float tclear = tclearance * vRect.width();
			boolean la = true;
			if (paintTextOutline.measureText(sb, 0, sb.length()) + offX + tclear > vRect.right - xm) {
				offX = -offX;
				la = false;
			}
			//offY += smarkerw / 2;
			offY -= max(ym + offY + paintTextOutline.descent() + tclear - vRect.bottom, 0);
			offY -= min(ym + offY + paintTextOutline.ascent() - tclear - vRect.top, 0);

			Util.formatTemp(sb, temp, d.tempUnit, d.noDigits);
			drawText(cvs, sb, xm + offX, ym + offY, la, true);
		}
	}
}
