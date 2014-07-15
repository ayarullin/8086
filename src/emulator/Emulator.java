package emulator;

import java.io.IOException;
import java.io.InputStream;

public class Emulator {
	
	private Memory mem;
	private Cpu cpu;
	
	private void init() {
		mem = new Memory(0x100000);
		
		try {
			String biosFileName = "bios/rombios.bin";
			InputStream fin = Emulator.class.getClassLoader().getResourceAsStream(biosFileName);
			byte[] buf = new byte[65536];
	        int k = 0;
	        while (k < buf.length) {
	            int t = fin.read(buf, k, buf.length - k);
	            if (t < 0)
	                break;
	            k += t;
	        }
	        fin.close();
	        byte[] xbuf = new byte[k];
	        System.arraycopy(buf, 0, xbuf, 0, k);
	        mem.loadData(0x100000 - k, xbuf);
		} catch (IOException e) {
			e.printStackTrace();
			// TODO: logger here;
		}
		
		cpu = new Cpu(mem);
	}
	
	private void run() throws Exception {
		while (true)
		{
			cpu.step();
		}
	}

	public static void main(String[] args) throws Exception {
		Emulator emulator = new Emulator();
		emulator.init();
		emulator.run();
	}
}
