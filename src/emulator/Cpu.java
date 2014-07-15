package emulator;

public class Cpu {

	private static final int regAX = 0;
	private static final int regCX = 1;
	private static final int regDX = 2;
	private static final int regBX = 3;
	private static final int regSP = 4;
	private static final int regBP = 5;
	private static final int regSI = 6;
	private static final int regDI = 7;
	
	private static final int regES = 0;
	private static final int regCS = 1;
	private static final int regSS = 2;
	private static final int regDS = 3;
	
	// flag masks
	private static final int flagCF = 0x0001;
	private static final int flagPF = 0x0004;
	private static final int flagAF = 0x0010;
	private static final int flagZF = 0x0040;
	private static final int flagSF = 0x0080;
	private static final int flagTF = 0x0100;
	private static final int flagIF = 0x0200;
	private static final int flagDF = 0x0400;
    private static final int flagOF = 0x0800;
	
	static final int INIT_CS = 0xf000;
    static final int INIT_IP = 0xfff0;
	
	private int[] reg;
	private int[] sreg;
	
	private int flags;
	
	private int ip;
	
	private int opcodeNum = 0;
	
	private Memory mem;
	
	private ModRM modRM;
	
	class ModRM {
		private byte regIdx;
		private byte memIdx;
		private Integer addr;
		
		public void read() throws Exception {
			byte modRM = nextByte();
			
			byte mode = (byte)((modRM >> 6) & 0x03);
			regIdx = (byte)((modRM >> 3) & 0x07);
			memIdx = (byte)(modRM & 0x07);
			
			switch (mode) {
				case 3:
					addr = null; // using memIdx as regIdx in mode 3
					break;
				default:
					throw new Exception("Unsupported mode: " + mode);
			}
		}
		
		public byte getMem8() {
			if (null == addr) {
				return (byte) getReg8(memIdx);
			}
			return mem.getByte(addr);
		}
		
		public void setMem8(byte value) {
			if (null == addr) {
				setReg8(memIdx, value);
			} else {
				mem.setByte(addr, value);
			}
		}
		
		public short getMem16() {
			if (null == addr) {
				return (short) reg[memIdx];
			}
			
			return mem.getWord(addr);
		}
		
		public void setMem16(short value) {
			if (null == addr) {
				reg[memIdx] = value & 0xffff;
			} else {
				mem.setWord(addr, value);
			}
		}
		
		public byte getReg8() {
			return getReg8(regIdx);
		}
		
		public void setReg8(byte value) {
			setReg8(regIdx, value);
		}
		
		private byte getReg8(byte index) {
			if ((index & 0x04) != 0) {
				return (byte) (reg[index & 0x03] >> 8);
			} else {
				return (byte) (reg[index & 0x03] & 0xff);
			}
		}
		
		private void setReg8(byte index, byte value) {
			if ((index & 0x04) != 0) {
				reg[index & 0x03] &= 0x00ff;
				reg[index & 0x03] |= (value << 8) & 0xff00;
			} else {
				reg[index & 0x03] &= 0xff00;
				reg[index & 0x03] |= value & 0x00ff;
			}
		}
		
		public int getReg16() {
			return reg[regIdx];
		}
		
		public void setSreg(short value) {
			sreg[regIdx] = value & 0xffff;
		}
	}
	
	public Cpu(Memory mem) {
		reg = new int[8];
		sreg = new int[4];
		
		sreg[regCS] = INIT_CS;
		ip = INIT_IP;
		
		this.mem = mem;
		
		modRM = new ModRM();
	}
	
	public void step() throws Exception {
		byte opcode = nextByte();
		
		System.out.println(String.format("%s: %X", ++opcodeNum, opcode));
		
		switch (opcode) {
			case (byte) 0x1E: // PUSH DS
				push(reg[regDS]);
				break;
			case (byte) 0x30: // XOR Eb Gb
				modRM.read();
				modRM.setMem8(xor8(modRM.getMem8(), modRM.getReg8()));
				break;
			case (byte) 0x31: // XOR Ev Gv
				modRM.read();
				modRM.setMem16(xor16(modRM.getMem16(), modRM.getReg16()));
				break;
			case (byte) 0x50: // PUSH AX
			case (byte) 0x51: // PUSH CX
			case (byte) 0x52: // PUSH DX
			case (byte) 0x53: // PUSH BX
			case (byte) 0x54: // PUSH SP
			case (byte) 0x55: // PUSH BP
			case (byte) 0x56: // PUSH SI
			case (byte) 0x57: // PUSH DI
				push(reg[opcode & 0x07]);
				break;
			case (byte) 0x8E: // MOV Sw Ew
				modRM.read();
				modRM.setSreg(modRM.getMem16());
				break;
			case (byte) 0xB0: // MOV AL Ib
			case (byte) 0xB1: // MOV CL Ib
			case (byte) 0xB2: // MOV DL Ib
			case (byte) 0xB3: // MOV BL Ib
				reg[opcode & 0x0f] &= 0xff00;
				reg[opcode & 0x0f] |= nextByte() & 0x00ff;
				break;
			case (byte) 0xB4: // MOV AH Ib
			case (byte) 0xB5: // MOV CH Ib
			case (byte) 0xB6: // MOV DH Ib
			case (byte) 0xB7: // MOV BH Ib
				reg[opcode & 0x03] &= 0x00ff;
				reg[opcode & 0x03] |= (nextByte() & 0x00ff) << 8;
				break;
			case (byte) 0xB8: // MOV AX Iv
			case (byte) 0xB9: // MOV CX Iv
			case (byte) 0xBA: // MOV DX Iv
			case (byte) 0xBB: // MOV BX Iv
			case (byte) 0xBC: // MOV SP Iv
			case (byte) 0xBD: // MOV BP Iv
			case (byte) 0xBE: // MOV SI Iv
			case (byte) 0xBF: // MOV DI Iv
				reg[opcode & 0x07] = nextWord();
				break;
			case (byte) 0xC7: // MOV Ev Iv
				modRM.read();
				modRM.setMem16(nextWord());
				break;
			case (byte) 0xE6: // OUT Ib AL
				outb(nextByte(), (byte) (reg[regAX] & 0xff));
				break;
			case (byte) 0xE8: // CALL Jv
				push(ip + 2);
				ip += nextWord() + 2;
				break;
			case (byte) 0xEA: // JMP Ap (far)
				opJmpAp();
				break;
			case (byte) 0xFA: // CLI
				setFlag(flagIF, false);
				break;
			default:
				//System.out.print(sreg[regSS] + " " + reg[regSP]);
				throw new InvalidOpcodeException(opcode);
		}
	}
	
	private byte xor8(byte v1, byte v2) {
		short shortResult = (short) (((short) v1 & 0xff) ^ ((short) v2 & 0xff));
		byte result = (byte) shortResult;
		
		setFlag(flagOF, false);
		setFlag(flagCF, false);
		setFlag(flagAF, false); // ??
		updateFlags8();
		
		return result;
	}
	
	private short xor16(int v1, int v2) {
		int intRes = (v1 & 0xffff) ^ (v2 & 0xffff);
		short result = (short) intRes;
		
		setFlag(flagOF, false);
		setFlag(flagCF, false);
		setFlag(flagAF, false); // ??
		updateFlags16();
		
		return result;
	}
	
	private void push(int value) {
		int sp = (reg[regSP] - 2) & 0xffff;
        reg[regSP] = sp;
        mem.setWord((sreg[regSS] << 4) + sp, (short) value);
	}
	
	private void outb(byte port, byte val) {
		// TODO: DMA implementation
		System.out.println(String.format("out 0x%X, 0x%X", port, val));
	}
	
	private void opJmpAp() {
		short newIP = nextWord();
		short newCS = nextWord();
		sreg[regCS] = newCS & 0xffff;
		ip = newIP & 0xffff;
	}
	
	public void updateFlags16() {
		// TODO: implementation
	}
	
	public void updateFlags8() {
		// TODO: implementation
	}
	
	private void setFlag(int mask, boolean value) {
		if (value) {
			flags |= mask;
		} else {
			flags &= ~mask;
		}
	}
	
	private byte nextByte() {
		byte result = mem.getByte((sreg[regCS] << 4) + ip);
		ip += 1;
		return result;
	}
	
	private short nextWord() {
		short result = mem.getWord((sreg[regCS] << 4) + ip);
		ip += 2;
		return result;
	}
}
