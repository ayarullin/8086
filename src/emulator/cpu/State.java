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
	
	// flag masks
	private static final int CARRY_FLAG_MASK = 0x0001;
	private static final int PARITY_FLAG_MASK = 0x0004;
	private static final int AUXILIARY_FLAG_MASK = 0x0010;
	private static final int ZERO_FLAG_MASK = 0x0040;
	private static final int SIGN_FLAG_MASK = 0x0080;
	private static final int TRAP_FLAG_MASK = 0x0100;
	private static final int INTERRUPT_FLAG_MASK = 0x0200;
	private static final int DIRECTION_FLAG_MASK = 0x0400;
    private static final int OVERFLOW_FLAG_MASK = 0x0800;
	
	private int[] reg;
	private int[] sreg;
	
	private int ip;
	
	private int flags;
	
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
	
	public void setFlags(int value) {
		flags = value;
	}
	
	public int getFlags() {
		return flags;
	}
	
	public boolean getCarryFlag() {
		return getFlag(CARRY_FLAG_MASK);
	}
	
	public boolean getParityFlag() {
		return getFlag(PARITY_FLAG_MASK);
	}
	
	public boolean getAuxiliaryFlag() {
		return getFlag(AUXILIARY_FLAG_MASK);
	}
	
	public boolean getZeroFlag() {
		return getFlag(ZERO_FLAG_MASK);
	}
	
	public boolean getSignFlag() {
		return getFlag(SIGN_FLAG_MASK);
	}
	
	public boolean getTrapFlag() {
		return getFlag(TRAP_FLAG_MASK);
	}
	
	public boolean getInterruptFlag() {
		return getFlag(INTERRUPT_FLAG_MASK);
	}
	
	public boolean getDirectionFlag() {
		return getFlag(DIRECTION_FLAG_MASK);
	}
	
	public boolean getOverflowFlag() {
		return getFlag(OVERFLOW_FLAG_MASK);
	}
	
	public void setCarryFlag(boolean value) {
		setFlag(CARRY_FLAG_MASK, value);
	}
	
	public void setParityFlag(boolean value) {
		setFlag(PARITY_FLAG_MASK, value);
	}
	
	public void setAuxiliaryFlag(boolean value) {
		setFlag(AUXILIARY_FLAG_MASK, value);
	}
	
	public void setZeroFlag(boolean value) {
		setFlag(ZERO_FLAG_MASK, value);
	}
	
	public void setSignFlag(boolean value) {
		setFlag(SIGN_FLAG_MASK, value);
	}
	
	public void setTrapFlag(boolean value) {
		setFlag(TRAP_FLAG_MASK, value);
	}
	
	public void setInterruptFlag(boolean value) {
		setFlag(INTERRUPT_FLAG_MASK, value);
	}
	
	public void setDirectionFlag(boolean value) {
		setFlag(DIRECTION_FLAG_MASK, value);
	}
	
	public void setOverflowFlag(boolean value) {
		setFlag(OVERFLOW_FLAG_MASK, value);
	}
	
	private void setFlag(int mask, boolean value) {
		if (value) {
			flags |= mask;
		} else {
			flags &= ~mask;
		}
	}
	
	private boolean getFlag(int mask) {
		return (flags & mask) == mask;
	}
	
	/**
	 * TODO: delete
	 * @param v
	 * @return
	 */
	private String byteToHex(int v)
	{
		return Integer.toHexString((v & 0xff) | 0x100).substring(1);
	}

	/** 
	 * TODO: delete
	 * @param v
	 * @return
	 */
	private String wordToHex(int v)
	{
		return Integer.toHexString((v & 0xffff) | 0x10000).substring(1);
	}
	
	public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("AX=");  sb.append(wordToHex(getAX()));
        sb.append(" BX="); sb.append(wordToHex(getBX()));
        sb.append(" CX="); sb.append(wordToHex(getCX()));
        sb.append(" DX="); sb.append(wordToHex(getDX()));
        sb.append(" SI="); sb.append(wordToHex(getSI()));
        sb.append(" DI="); sb.append(wordToHex(getDI()));
        sb.append(" BP="); sb.append(wordToHex(getBP()));
        sb.append(" SP="); sb.append(wordToHex(getSP()));
        sb.append(" DS="); sb.append(wordToHex(getDS()));
        sb.append(" ES="); sb.append(wordToHex(getES()));
        sb.append(" SS="); sb.append(wordToHex(getSS()));
        sb.append(" flags="); sb.append(wordToHex(flags));
        sb.append(" (");
        sb.append((flags & OVERFLOW_FLAG_MASK) == 0 ? ' ' : 'O');
        sb.append((flags & DIRECTION_FLAG_MASK) == 0 ? ' ' : 'D');
        sb.append((flags & INTERRUPT_FLAG_MASK) == 0 ? ' ' : 'I');
        sb.append((flags & TRAP_FLAG_MASK) == 0 ? ' ' : 'T');
        sb.append((flags & SIGN_FLAG_MASK) == 0 ? ' ' : 'S');
        sb.append((flags & ZERO_FLAG_MASK) == 0 ? ' ' : 'Z');
        sb.append((flags & AUXILIARY_FLAG_MASK) == 0 ? ' ' : 'A');
        sb.append((flags & PARITY_FLAG_MASK) == 0 ? ' ' : 'P');
        sb.append((flags & CARRY_FLAG_MASK) == 0 ? ' ' : 'C');
        sb.append(")");
        sb.append(" CS:IP=");
        sb.append(wordToHex(getCS()));
        sb.append(":");
        sb.append(wordToHex(getIP() - 1));
        return sb.toString();
    }
}
