package emulator.cpu;

public class State {
	
	public static final int AX_INDEX = 0;
	public static final int CX_INDEX = 1;
	public static final int DX_INDEX = 2;
	public static final int BX_INDEX = 3;
	public static final int SP_INDEX = 4;
	public static final int BP_INDEX = 5;
	public static final int SI_INDEX = 6;
	public static final int DI_INDEX = 7;
	
	public static final int ES_INDEX = 0;
	public static final int CS_INDEX = 1;
	public static final int SS_INDEX = 2;
	public static final int DS_INDEX = 3;
	
	private int[] reg;
	private int[] sreg;
	
	private int ip;
	
	public State() {
		reg = new int[8];
		sreg = new int[4];
		reset();
	}
	
	public void reset() {
		for (int i = 0; i < 8; ++i) {
			reg[i] = 0;
		}
		
		for (int i = 0; i < 4; ++i) {
			sreg[i] = 0;
		}
	}
	
	public int getAX() {
		return reg[AX_INDEX];
	}
	
	public int getCX() {
		return reg[CX_INDEX];
	}
	
	public int getDX() {
		return reg[DX_INDEX];
	}
	
	public int getBX() {
		return reg[BX_INDEX];
	}
	
	public int getSP() {
		return reg[SP_INDEX];
	}
	
	public int getBP() {
		return reg[BP_INDEX];
	}
	
	public int getSI() {
		return reg[SI_INDEX];
	}
	
	public int getDI() {
		return reg[DI_INDEX];
	}
	
	public int getES() {
		return sreg[ES_INDEX];
	}
	
	public int getCS() {
		return sreg[CS_INDEX];
	}
	
	public int getSS() {
		return sreg[SS_INDEX];
	}
	
	public int getDS() {
		return sreg[DS_INDEX];
	}
	
	public int getReg(int index) {
		return reg[index];
	}
	
	public int getSegReg(int index) {
		return sreg[index];
	}
	
	public void setAX(int v) {
		reg[AX_INDEX] = v & 0xffff;
	}
	
	public void setCX(int v) {
		reg[CX_INDEX] = v & 0xffff;
	}

	public void setDX(int v) {
		reg[DX_INDEX] = v & 0xffff;
	}
	
	public void setBX(int v) {
		reg[BX_INDEX] = v & 0xffff;
	}
	
	public void setSP(int v) {
		reg[SP_INDEX] = v & 0xffff;
	}

	public void setBP(int v) {
		reg[BP_INDEX] = v & 0xffff;
	}

	public void setSI(int v) {
		reg[SI_INDEX] = v & 0xffff;
	}

	public void setDI(int v) {
		reg[DI_INDEX] = v & 0xffff;
	}

	public void setES(int v) {
		sreg[ES_INDEX] = v & 0xffff;
	}
	
	public void setCS(int v) {
		sreg[CS_INDEX] = v & 0xffff;
	}
	
	public void setSS(int v) {
		sreg[SS_INDEX] = v & 0xffff;
	}
	
	public void setDS(int v) {
		sreg[DS_INDEX] = v & 0xffff;
	}
	
	public void setReg(int index, int v) {
		reg[index] = v & 0xffff;
	}
	
	public void setSegReg(int index, int v) {
		sreg[index] = v & 0xffff;
	}
	
	public int getIP() {
		return ip;
	}
	
	public void setIP(int v) {
		ip = v & 0xffff;
	}
	
	
	
	public byte getAL() {
		return (byte) reg[AX_INDEX];
	}
	
	
	
	
	
	public byte getReg8(int index) {
		if ((index & 0x04) != 0) {
			return (byte) (reg[index & 0x03] >> 8);
		} else {
			return (byte) (reg[index & 0x03] & 0xff);
		}
	}
	
	public void setReg8(int index, byte value) {
		if ((index & 0x04) != 0) {
			reg[index & 0x03] &= 0x00ff;
			reg[index & 0x03] |= (value << 8) & 0xff00;
		} else {
			reg[index & 0x03] &= 0xff00;
			reg[index & 0x03] |= value & 0x00ff;
		}
	}
}
