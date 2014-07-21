package emulator;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
	
	private static final int INIT_CS = 0xf000;
    private static final int INIT_IP = 0xfff0;
    private static final int INIT_FLAGS = 0xf002;
	
	private int[] reg;
	private int[] sreg;
	
	private int flags;
	
	private int ip;
	private int jump = -1;
	
	private int opcodeNum = 0;
	
	private Memory mem;
	
	private ModRM modRM;
	
	private final static Logger logger = Logger.getAnonymousLogger();
	
	class ModRM {
		private byte regIdx;
		private byte memIdx;
		private Integer addr;
		
		private Integer forcedSegIdx = null;
		
		public void read() throws Exception {
			byte modRM = nextByte();
			
			byte mode = (byte)((modRM >> 6) & 0x03);
			regIdx = (byte)((modRM >> 3) & 0x07);
			memIdx = (byte)(modRM & 0x07);
			
			switch (mode) {
				case 0:
					addr = getMode0Address();
					break;
				case 1:
					addr = getMode1Address();
					break;
				case 2: 
					addr = getMode2Address();
					break;
				case 3:
					addr = null; // using memIdx as regIdx in mode 3
					break;
				default:
					throw new Exception("Unsupported mode: " + mode);
			}
		}
		
		private int getMode0Address() {
			switch (memIdx) {
				case 0:
					return getAddr(regDS, (short) (reg[regBX] + reg[regSI]));
				case 1:
					return getAddr(regDS, (short) (reg[regBX] + reg[regDI]));
				case 2:
					return getAddr(regSS, (short) (reg[regBP] + reg[regSI]));
				case 3:
					return getAddr(regSS, (short) (reg[regBP] + reg[regDI]));
				case 4:
					return getAddr(regDS, (short) (reg[regSI]));
				case 5:
					return getAddr(regDS, (short) (reg[regDI]));
				case 6:
					return getAddr(regSS, nextWord());
				case 7:
					return getAddr(regDS, (short) (reg[regBX]));
				default:
					throw new RuntimeException("Unknown memIdx: " + memIdx);
			}
		}
		
		private int getMode1Address() {
			switch (memIdx) {
				case 0:
					return getAddr(regDS, (short) (reg[regBX] + reg[regSI] + nextByte()));
				case 1:
					return getAddr(regDS, (short) (reg[regBX] + reg[regDI] + nextByte()));
				case 2:
					return getAddr(regSS, (short) (reg[regBP] + reg[regSI] + nextByte()));
				case 3:
					return getAddr(regSS, (short) (reg[regBP] + reg[regDI] + nextByte()));
				case 4:
					return getAddr(regDS, (short) (reg[regSI] + nextByte()));
				case 5:
					return getAddr(regDS, (short) (reg[regDI] + nextByte()));
				case 6:
					return getAddr(regSS, (short) (reg[regBP] + nextByte()));
				case 7:
					return getAddr(regDS, (short) (reg[regBX] + nextByte()));
				default:
					throw new RuntimeException("Unknown memIdx: " + memIdx);
			}
		}
		
		private int getMode2Address() {
			switch (memIdx) {
				case 0:
					return getAddr(regDS, (short) (reg[regBX] + reg[regSI] + nextWord()));
				case 1:
					return getAddr(regDS, (short) (reg[regBX] + reg[regDI] + nextWord()));
				case 2:
					return getAddr(regSS, (short) (reg[regBP] + reg[regSI] + nextWord()));
				case 3:
					return getAddr(regSS, (short) (reg[regBP] + reg[regDI] + nextWord()));
				case 4:
					return getAddr(regDS, (short) (reg[regSI] + nextWord()));
				case 5:
					return getAddr(regDS, (short) (reg[regDI] + nextWord()));
				case 6:
					return getAddr(regSS, (short) (reg[regBP] + nextWord()));
				case 7:
					return getAddr(regDS, (short) (reg[regBX] + nextWord()));
				default:
					throw new RuntimeException("Unknown memIdx: " + memIdx);
			}
		}
		
		private int getAddr(int segIndex, int offs) {
			if (null != forcedSegIdx) {
				segIndex = forcedSegIdx;
				forcedSegIdx = null;
			}
			return (sreg[segIndex] << 4) + (offs & 0xffff);
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
		
		public int getSreg() {
			return sreg[regIdx];
		}
		
		public byte getRegIdx() {
			return regIdx;
		}
		
		public void forceSeg(int regIdx) {
			forcedSegIdx = regIdx;
		}
	}
	
	class LogFormatter extends Formatter {

		@Override
		public String format(LogRecord record) {
			return record.getMessage() + "\n";
		}
	}
	
	public Cpu(Memory mem) throws SecurityException, IOException {
		reg = new int[8];
		sreg = new int[4];
		
		sreg[regCS] = INIT_CS;
		ip = INIT_IP;
		
		flags = INIT_FLAGS;
		
		this.mem = mem;
		
		modRM = new ModRM();
		
		logger.setUseParentHandlers(false);
		Handler logHandler = new FileHandler("cpu.log");
		logHandler.setFormatter(new LogFormatter());
		logger.addHandler(logHandler);
	}
	
	private String byteToHex(int v)
	{
		return Integer.toHexString((v & 0xff) | 0x100).substring(1);
	}

	private String wordToHex(int v)
	{
		return Integer.toHexString((v & 0xffff) | 0x10000).substring(1);
	}
	
	private String getStateString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(" AX=");  sb.append(wordToHex(reg[regAX]));
        sb.append("  BX="); sb.append(wordToHex(reg[regBX]));
        sb.append("  CX="); sb.append(wordToHex(reg[regCX]));
        sb.append("  DX="); sb.append(wordToHex(reg[regDX]));
        sb.append("  SI="); sb.append(wordToHex(reg[regSI]));
        sb.append("  DI="); sb.append(wordToHex(reg[regDI]));
        sb.append("  BP="); sb.append(wordToHex(reg[regBP]));
        sb.append("  SP="); sb.append(wordToHex(reg[regSP]));
        //sb.append("\n");
        sb.append(" DS="); sb.append(wordToHex(sreg[regDS]));
        sb.append("  ES="); sb.append(wordToHex(sreg[regES]));
        sb.append("  SS="); sb.append(wordToHex(sreg[regSS]));
        sb.append("  flags="); sb.append(wordToHex(flags));
        sb.append(" (");
        sb.append((flags & flagOF) == 0 ? ' ' : 'O');
        sb.append((flags & flagDF) == 0 ? ' ' : 'D');
        sb.append((flags & flagIF) == 0 ? ' ' : 'I');
        sb.append((flags & flagTF) == 0 ? ' ' : 'T');
        sb.append((flags & flagSF) == 0 ? ' ' : 'S');
        sb.append((flags & flagZF) == 0 ? ' ' : 'Z');
        sb.append((flags & flagAF) == 0 ? ' ' : 'A');
        sb.append((flags & flagPF) == 0 ? ' ' : 'P');
        sb.append((flags & flagCF) == 0 ? ' ' : 'C');
        sb.append(")");
        //sb.append(")  cycl="); sb.append(cycl);
        //sb.append("\n");
        sb.append(" CS:IP=");
        sb.append(wordToHex(sreg[regCS]));
        sb.append(":");
        sb.append(wordToHex(ip - 1));
        sb.append(" ");
        for (int i = 0; i < 16; i++) {
            //sb.append((ip + i == nextip) ? '|' : ' ');
        	sb.append(' ');
            sb.append(byteToHex(
              mem.getByte((sreg[regCS] << 4) + ip + i - 1)));
        }
        //sb.append("\n");
        return sb.toString();
    }
	
	public void step() throws Exception {
		byte opcode = nextByte();
		
		logger.info(String.format("%s: 0x%X ", ++opcodeNum, opcode) + getStateString());
		if (opcodeNum > 100000) {
			System.exit(0);
		}
		
		switch (opcode) {
			case (byte) 0x06: // PUSH ES
				push(sreg[regES]);
				break;
			case (byte) 0x07: // POP ES
				sreg[regES] = pop();
				break;
			case (byte) 0x1E: // PUSH DS
				push(sreg[regDS]);
				break;
			case (byte) 0x1F: // POP DS
				sreg[regDS] = pop();
				break;
			case (byte) 0x2E: // CS:
				modRM.forceSeg(regCS);
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
			case (byte) 0x58: // POP AX
			case (byte) 0x59: // POP CD
			case (byte) 0x5A: // POP DX
			case (byte) 0x5B: // POP BX
			case (byte) 0x5C: // POP SP
			case (byte) 0x5D: // POP BP
			case (byte) 0x5E: // POP SI
			case (byte) 0x5F: // POP DI
				reg[opcode & 0x07] = pop();
				break;
			case (byte) 0x72: // JB Jb
				if (getFlag(flagCF)) {
					ip = ip + nextByte() + 1;
				} else {
					nextByte();
				}
				break;
			case (byte) 0x77: // JA Jb
				if (!getFlag(flagCF) && !getFlag(flagZF)) {
					ip = ip + nextByte() + 1;
				} else {
					nextByte();
				}
				break;
			case (byte) 0x80: // GRP1 Eb Ib
				modRM.read();
				switch (modRM.getRegIdx()) {
					case 7: // CMP
						sub8(modRM.getMem8(), nextByte());
						break;
					default:
						throw new RuntimeException("Invalid regIdx: " + modRM.getRegIdx());
				}
				break;
			case (byte) 0x81: // GRP1 Ev Iv
				modRM.read();
				switch (modRM.getRegIdx()) {
					case 0: // ADD
						modRM.setMem16(add16(modRM.getMem16(), nextWord()));
						break;
//					case 1:
//					case 2:
//					case 3:
					case 4: // AND
						modRM.setMem16(and16(modRM.getMem16(), nextWord()));
						break;
//					case 5:
//					case 6:
					case 7: //CMP
						sub16(modRM.getMem16(), nextWord());
						break;
					default:
						throw new RuntimeException("Invalid regIdx: " + modRM.getRegIdx());
				}
				break;
			case (byte) 0x88: //MOV Eb Gb
				modRM.read();
				modRM.setMem8(modRM.getReg8());
				break;
			case (byte) 0x89: // MOV Ev Gv
				modRM.read();
				modRM.setMem16((short) modRM.getReg16());
				break;
			case (byte) 0x8C: // MOV Ew Sw
				modRM.read();
				modRM.setMem16((short)modRM.getSreg());
				break;
			case (byte) 0x8E: // MOV Sw Ew
				modRM.read();
				modRM.setSreg(modRM.getMem16());
				break;
			case (byte) 0xAA: // STOSB
			case (byte) 0xAB: // STOSW
			case (byte) 0xAC: // LODSB
			case (byte) 0xAD: // LODSW
			case (byte) 0xAE: // SCASB
			case (byte) 0xAF: // SCASW
				processString(opcode);
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
			case (byte) 0xC3: // RET
				ip = pop() & 0xffff;
				break;
			case (byte) 0xC6: // MOV Eb Ib
				modRM.read();
				modRM.setMem8(nextByte());
				break;
			case (byte) 0xC7: // MOV Ev Iv
				modRM.read();
				modRM.setMem16(nextWord());
				break;
			case (byte) 0xCD: // INT Ib
				interrupt(nextByte());
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
			case (byte) 0xD1: // GRP2 Ev 1
				modRM.read();
				switch (modRM.getRegIdx()) {
					case 4:
						modRM.setMem16(shl16(modRM.getMem16(), 1));
						break;
					default:
						throw new RuntimeException("Invalid regIdx: " + modRM.getRegIdx());
				}
				break;
			case (byte) 0xF3: // REPZ
				jump = ip - 1;
				break;
			case (byte) 0xFA: // CLI
				setFlag(flagIF, false);
				break;
			case (byte) 0xFB: // STI
				setFlag(flagIF, true);
				break;
			case (byte) 0xFC: // CLD
				setFlag(flagDF, false);
				break;
			case (byte) 0xFD: // STD
				setFlag(flagDF, true);
				break;
			case (byte) 0xFF: // GRP5 Ev
				modRM.read();
				switch (modRM.getRegIdx()) {
					case 4: // JMP 
						ip = modRM.getMem16() & 0xFFFF;
						break;
					case 6: // PUSH
						push(modRM.getMem16()); 
						break;
					default:
						throw new RuntimeException("Invalid regIdx: " + modRM.getRegIdx());
				}
				break;
			default:
				//System.out.print(sreg[regSS] + " " + reg[regSP]);
				throw new InvalidOpcodeException(opcode);
		}
	}
	
	private void processString(byte opcode) throws InvalidOpcodeException {
		
		int diff = (getFlag(flagDF) ? -1 : 1) << (opcode & 1);
		
		switch (opcode) {
			case (byte) 0xAB: // STOSW
				mem.setWord((sreg[regES] << 4) + reg[regDI], (short) reg[regAX]);
				reg[regDI] = reg[regDI] + diff;
				break;
			default:
				throw new InvalidOpcodeException(opcode);
		}
		
		if (jump > 0) {
			reg[regCX] -= 1;
			if (reg[regCX] > 0) {
				ip = jump;
			}
		}
	}
	
	private short add16(int v1, int v2) {
		updateFlags16((v1 & 0xffff) + (v2 & 0xffff));
		int intRes = (short) v1 + (short) v2;
		setFlag(flagOF, intRes > 0x7fff || intRes < -0x8000);
		setFlag(flagAF, (v1 & 0xf) + (v2 & 0xf) > 0xf);
		return (short) intRes;
	}
	
	private short sub8(byte v1, byte v2) {
		updateFlags8((short) ((v1 & 0xff) - (v2 & 0xff)));
		short shortResult = (short) (v1 - v2);
		setFlag(flagOF, shortResult > 0x7f || shortResult < -0x80);
		setFlag(flagAF, (v1 & 0xf) < (v2 & 0xf));
		return (byte) shortResult;
	}
	
	private short sub16(int v1, int v2) {
		updateFlags16((v1 & 0xffff) - (v2 & 0xffff));
		int intRes = (short) v1 - (short) v2;
		setFlag(flagOF, intRes > 0x7fff || intRes < -0x8000);
		setFlag(flagAF, (v1 & 0xf) < (v2 & 0xf));
		return (short) intRes;
	}
	
	private short and16(int v1, int v2) {
		int intRes = (v1 & 0xffff) & (v2 & 0xffff);
		short shortRes = (short) intRes;
		updateFlags16(intRes);
		setFlag(flagOF, false);
		return shortRes;
	}
	
	private byte xor8(byte v1, byte v2) {
		short shortResult = (short) (((short) v1 & 0xff) ^ ((short) v2 & 0xff));
		byte result = (byte) shortResult;
		
		setFlag(flagOF, false);
		setFlag(flagCF, false);
		setFlag(flagAF, false); // ??
		updateFlags8(shortResult);
		
		return result;
	}
	
	private short xor16(int v1, int v2) {
		int intRes = (v1 & 0xffff) ^ (v2 & 0xffff);
		short result = (short) intRes;
		
		setFlag(flagOF, false);
		setFlag(flagCF, false);
		setFlag(flagAF, false); // ??
		updateFlags16(intRes);
		
		return result;
	}
	
	private short shl16(int v, int count) {
		v <<= count;
		
		updateFlags16(v);
		setFlag(flagCF, (v & 0x10000) == 0x10000);
		setFlag(flagAF, (v & 0x10) != 0);
		setFlag(flagOF, ((v >> 16) & 0x1) != ((v >> 15) & 0x1));
		
		return (short) v;
	}
	
	private void push(int value) {
		int sp = (reg[regSP] - 2) & 0xffff;
        reg[regSP] = sp;
        mem.setWord((sreg[regSS] << 4) + sp, (short) value);
	}
	
	private short pop() {
		short v = mem.getWord((sreg[regSS] << 4) + reg[regSP]);
		reg[regSP] = (short) (reg[regSP] + 2);
		return v;
	}
	
	private void outb(byte port, byte val) {
		// TODO: DMA implementation
		System.out.println(String.format("out 0x%X, 0x%X", port, val));
	}
	
	private void interrupt(byte intNo) {
		push(flags);
		push(sreg[regCS]);
		push(ip);
		ip = mem.getWord(4 * intNo) & 0xffff;
		sreg[regCS] = mem.getWord(4 * intNo + 2) & 0xffff;
		System.out.println(String.format("int: 0x%X", intNo));
	}
	
	private void opJmpAp() {
		short newIP = nextWord();
		short newCS = nextWord();
		sreg[regCS] = newCS & 0xffff;
		ip = newIP & 0xffff;
	}
	
	public void updateFlags16(int v) {
		setFlag(flagCF, (v & 0xFFFF0000) != 0);
		setFlag(flagZF, (short) v == 0);
		
		byte byteVal = (byte) v;
		byte bitSum = 0;
		for (byte b = 1; b != 0; b <<= 1) {
			if ((byteVal & b) != 0) {
				bitSum += 1;
			}
		}
		setFlag(flagPF, bitSum % 2 == 0);
		
		setFlag(flagSF, ((short) v & 0x8000) != 0);
	}
	
	public void updateFlags8(short v) {
		setFlag(flagCF, (v & 0xFF00) != 0);
		setFlag(flagZF, v == 0);
		
		byte bitSum = 0;
		for (byte b = 1; b != 0; b <<= 1) {
			if ((v & b) != 0) {
				bitSum += 1;
			}
		}
		setFlag(flagPF, bitSum % 2 == 0);

		setFlag(flagSF, (v & 0x80) != 0);
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
