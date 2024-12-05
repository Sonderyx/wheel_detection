package be.ntmn.inficam;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import be.ntmn.libinficam.InfiCam;

public abstract class Palette {
	public int name;

	public Palette(int name) { this.name = name; }

	private static class Pixel {
		double r, g, b;

		Pixel(double r, double g, double b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}
	}

	abstract Pixel func(double x);

	private static final Palette WhiteHot = new Palette(R.string.palette_whitehot) {
		@Override
		public Pixel func(double x) {
			return new Pixel(x, x, x);
		}
	};

	private static final Palette BlackHot = new Palette(R.string.palette_blackhot) {
		@Override
		public Pixel func(double x) {
			return new Pixel(1 - x, 1 - x, 1 - x);
		}
	};

	private static final Palette RainbowBW = new Palette(R.string.palette_rainbowbw) {
		@Override
		Pixel func(double x) {return getRainbowBW(x);}
	};

	private static final Palette Arctic = new Palette(R.string.palette_arctic) {
		@Override
		Pixel func(double x) {return getArctic(x);}
	};

	private static final Palette RedHot = new Palette(R.string.palette_redhot) {
		@Override
		public Pixel func(double x) {
			return new Pixel(x, 0, 0);
		}
	};

	private static final Palette RedCold = new Palette(R.string.palette_redcold) {
		@Override
		public Pixel func(double x) {
			return new Pixel(1-x, 0, 0);
		}
	};

	private static final Palette GreenHot = new Palette(R.string.palette_greenhot) {
		@Override
		public Pixel func(double x) {
			return new Pixel(0, x, 0);
		}
	};

	private static final Palette GreenCold = new Palette(R.string.palette_greencold) {
		@Override
		public Pixel func(double x) {
			return new Pixel(0, 1 - x, 0);
		}
	};

	private static final Palette Iron = new Palette(R.string.palette_iron) {
		@Override
		Pixel func(double x) {return getIron(x);}
		//public Pixel func(double x) {return new Pixel(sqrt(x), pow(x, 3), max(0.0, sin(2.0 * PI * x)));}
	};
	private static final Palette Lava = new Palette(R.string.palette_lava) {
		@Override
		Pixel func(double x) {return getLava(x);}
	};

	private static final Palette Rainbow = new Palette(R.string.palette_rainbow) {
		@Override
		Pixel func(double x) {return getRainbow(x);}
		//Pixel func(double x) {return hsvPixel((1 - x) * 360.0, 1, 1);}
	};

	private static final Palette Rainbow2 = new Palette(R.string.palette_rainbow2) {
		@Override
		Pixel func(double x) {
			return hsvPixel((1 - x) * 270.0, 1, 1);
		}
	};

	public static Palette[] palettes = new Palette[] {
			WhiteHot, BlackHot, Iron, Arctic, Lava, Rainbow, RainbowBW
	};

	private static Pixel hsvPixel(double h, double s, double v) {
		double r, g, b;
		double c = s * v;
		double y = c * (1 - abs((h / 60.0) % 2 - 1));
		double m = v - c;
		if (h >= 0 && h < 60) {
			r = c; g = y; b = 0;
		} else if (h >= 60 && h < 120) {
			r = y; g = c; b = 0;
		} else if (h >= 120 && h < 180) {
			r = 0; g = c; b = y;
		} else if (h >= 180 && h < 240) {
			r = 0; g = y; b = c;
		} else if(h >= 240 && h < 300) {
			r = y; g = 0; b = c;
		} else {
			r = c; g = 0; b = y;
		}
		return new Pixel(r + m, g + m, b + m);
	}

	private static Pixel getRainbowBW(double x) {
		Pixel pixel;
		if (x<0.45)      pixel=getMedRGB(245,  0,245,  0,245,  0,x,0.00,0.45);
		else if (x<0.60) pixel=getMedRGB(  0, 10,  0,100,  0,240,x,0.45,0.60);
		else if (x<0.65) pixel=getMedRGB( 10, 30,100,140,240,150,x,0.60,0.65);
		else if (x<0.70) pixel=getMedRGB( 30, 80,140,170,150, 60,x,0.65,0.70);
		else if (x<0.75) pixel=getMedRGB( 80,145,170,195, 60, 27,x,0.70,0.75);
		else if (x<0.80) pixel=getMedRGB(145,200,195,210, 27, 25,x,0.75,0.80);
		else if (x<0.85) pixel=getMedRGB(200,240,210,200, 25, 35,x,0.80,0.85);
		else if (x<0.90) pixel=getMedRGB(240,245,200,145, 35, 45,x,0.85,0.90);
		else if (x<0.95) pixel=getMedRGB(245,240,145,100, 45, 50,x,0.90,0.95);
		else             pixel=getMedRGB(240,230,100, 30, 50, 70,x,0.95,1.00);

		return pixel;//new Pixel(pixel.r,pixel.g,pixel.b);
	}

	private static Pixel getIron(double x) {
		Pixel pixel;
		if (x<0.086)      pixel=getMedRGB(  0, 35, 15,  1, 21,110,x,0.000,0.086);
		else if (x<0.216) pixel=getMedRGB( 35,122,  1,  5,110,155,x,0.086,0.216);
		else if (x<0.433) pixel=getMedRGB(122,213,  5, 68,155, 95,x,0.216,0.433);
		else if (x<0.650) pixel=getMedRGB(213,251, 68,160, 95, 19,x,0.433,0.650);
		else if (x<0.866) pixel=getMedRGB(251,252,160,239, 19,111,x,0.650,0.866);
		else if (x<0.952) pixel=getMedRGB(252,248,239,253,111,185,x,0.866,0.952);
		else			  pixel=getMedRGB(248,245,253,255,185,255,x,0.952,1.000);
		return pixel;
	}

	private static Pixel getArctic(double x) {
		Pixel pixel;
		if (x<0.111)      pixel=getMedRGB(  2,  5,  3,  7,133,228,x,0.000,0.111);
		else if (x<0.310) pixel=getMedRGB(  5, 68,  7,238,228,241,x,0.111,0.310);
		else if (x<0.505) pixel=getMedRGB( 68,106,238,100,241, 96,x,0.310,0.505);
		else if (x<0.692) pixel=getMedRGB(106,238,100,104, 96, 17,x,0.505,0.692);
		else if (x<0.857) pixel=getMedRGB(238,249,104,218, 17, 41,x,0.692,0.857);
		else if (x<0.982) pixel=getMedRGB(249,254,218,248, 41,210,x,0.857,0.982);
		else			  pixel=getMedRGB(254,254,248,250,210,213,x,0.982,1.000);
		return pixel;//new Pixel(pixel.r,pixel.g,pixel.b);
	}

	private static Pixel getLava(double x) {
		Pixel pixel;

		if (x<0.094)      pixel=getMedRGB( 0,  35,  0, 70,  0,150,x,0.000,0.094);
		else if (x<0.165) pixel=getMedRGB( 35, 10, 70, 97,150,154,x,0.094,0.165);
		else if (x<0.335) pixel=getMedRGB( 10, 12, 97,135,154,130,x,0.165,0.335);
		else if (x<0.424) pixel=getMedRGB( 12,119,135, 52,130,115,x,0.335,0.424);
		else if (x<0.509) pixel=getMedRGB(119,164, 52, 36,115, 69,x,0.424,0.509);
		else if (x<0.58)  pixel=getMedRGB(164,190, 36, 30, 69, 44,x,0.509,0.580);
		else if (x<0.634) pixel=getMedRGB(190,221, 30, 52, 44, 45,x,0.580,0.634);
		else if (x<0.754) pixel=getMedRGB(221,240, 52,107, 45, 16,x,0.634,0.754);
		else if (x<0.915) pixel=getMedRGB(240,249,107,219, 16, 37,x,0.754,0.915);
		else			  pixel=getMedRGB(249,255,219,255, 37,255,x,0.915,1.000);

		return pixel;//new Pixel(pixel.r,pixel.g,pixel.b);
	}

	private static Pixel getRainbow(double x) {
		Pixel pixel;

		if (x<0.045)      pixel=getMedRGB(  1,  2,  3, 11, 64, 80,x,0.000,0.045);
		else if (x<0.089) pixel=getMedRGB(  2,  6, 11, 42, 80,114,x,0.045,0.089);
		else if (x<0.245) pixel=getMedRGB(  6, 11, 42,107,114,205,x,0.089,0.245);
		else if (x<0.348) pixel=getMedRGB( 11, 33,107,148,205,121,x,0.245,0.348);
		else if (x<0.446) pixel=getMedRGB( 33,154,148,196,121, 24,x,0.348,0.446);
		else if (x<0.527) pixel=getMedRGB(154,221,196,211, 24, 28,x,0.446,0.527);
		else if (x<0.640) pixel=getMedRGB(221,245,211,167, 28, 39,x,0.527,0.640);
		else if (x<0.740) pixel=getMedRGB(245,239,167, 78, 39, 56,x,0.640,0.740);
		else if (x<0.803) pixel=getMedRGB(239,230, 78, 36, 56, 79,x,0.740,0.803);
		else if (x<0.902) pixel=getMedRGB(230,243, 36,138, 79,135,x,0.803,0.902);
		else			  pixel=getMedRGB(243,253,138,231,135,209,x,0.902,1.000);
		return pixel;//new Pixel(pixel.r,pixel.g,pixel.b);
	}

	public int[] getData() {
		byte[] palette = new byte[InfiCam.paletteLen * 4];
		for (int i = 0; i + 4 <= palette.length; i += 4) {
			double x = (float) i / (float) palette.length;
			Pixel pixel = func(x);
			palette[i + 0] = (byte) round(255.0 * pixel.r);
			palette[i + 1] = (byte) round(255.0 * pixel.g);
			palette[i + 2] = (byte) round(255.0 * pixel.b);
			palette[i + 3] = (byte) 255;
		}
		IntBuffer ib = ByteBuffer.wrap(palette).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
		int[] intPalette = new int[ib.remaining()];
		ib.get(intPalette);
		return intPalette;
	}
	private static Pixel getMedRGB(double r0, double r1, double g0, double g1, double b0, double b1, double pos, double start, double end) {
		r0=getMedVal(r0,r1,pos,start,end)/255;
		g0=getMedVal(g0,g1,pos,start,end)/255;
		b0=getMedVal(b0,b1,pos,start,end)/255;
		return new Pixel(r0, g0, b0);
	}

	//	private static double getMedVal(double y0, double y1, double x, double step) {
//		return y0 + (y1 - y0) * (x % step) / step;
//	}
	private static double getMedVal(double y0, double y1, double pos, double start, double end) {
		return y0 + (y1 - y0) * (pos-start) / (end-start);
	}
}
//overtemperature protection  show palette
