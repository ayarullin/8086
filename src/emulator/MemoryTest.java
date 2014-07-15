package emulator;

import static org.junit.Assert.*;

import org.junit.Test;

public class MemoryTest {

	@Test
	public void testByte() {
		Memory mem = new Memory(0x100000);
		
		byte a = 123;
		int addr = 463278;
		mem.setByte(addr, a);
		assertEquals(mem.getByte(addr), a);
		
		a = 0;
		addr = 36425;
		mem.setByte(addr, a);
		assertEquals(mem.getByte(addr), a);
		
		a = -10;
		addr = 1327;
		mem.setByte(addr, a);
		assertEquals(mem.getByte(addr), a);
	}

	@Test
	public void testWord() {
		Memory mem = new Memory(0x100000);
		
		short a = 12635;
		int addr = 273894;
		mem.setWord(addr, a);
		assertEquals(mem.getWord(addr), a);
		
		a = -12635;
		addr = 17364;
		mem.setWord(addr, a);
		assertEquals(mem.getWord(addr), a);
		
		// test with same address
		a = 1;
		mem.setWord(addr, a);
		assertEquals(mem.getWord(addr), a);
	}
	
	@Test
	public void testMix() {
		Memory mem = new Memory(0x100000);
		
		short a = 30000;
		int addr = 37261;
		mem.setWord(addr, a);
		assertEquals(mem.getByte(addr), a & 0xff);
		assertEquals(mem.getByte(addr + 1), a >> 8);
	}
	
	@Test
	public void testLoad() {
		Memory mem = new Memory(0x100000);
		
		int addr = 1239;
		byte[] data = {1, 2, 3, 4, 5, 89, -98, 123, 4};
		
		mem.loadData(addr, data);
		
		assertEquals(mem.getByte(addr), 1);
		assertEquals(mem.getByte(addr + 1), 2);
		assertEquals(mem.getWord(addr + 2), (4 << 8) | 3); //???
	}
}
