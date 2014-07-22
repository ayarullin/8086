package emulator.cpu;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import emulator.Memory;

public class Cpu {
	
	private State state;
	
	private static final int INIT_CS = 0xf000;
    private static final int INIT_IP = 0xfff0;
    private static final int INIT_FLAGS = 0xf002;
			
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
					return getAddr(State.DS_INDEX, (short) (state.getBX() + state.getSI()));
				case 1:
					return getAddr(State.DS_INDEX, (short) (state.getBX() + state.getDI()));
				case 2:
					return getAddr(State.SS_INDEX, (short) (state.getBP() + state.getSI()));
				case 3:
					return getAddr(State.SS_INDEX, (short) (state.getBP() + state.getDI()));
				case 4:
					return getAddr(State.DS_INDEX, (short) (state.getSI()));
				case 5:
					return getAddr(State.DS_INDEX, (short) (state.getDI()));
				case 6:
					return getAddr(State.SS_INDEX, nextWord());
				case 7:
					return getAddr(State.DS_INDEX, (short) (state.getBX()));
				default:
					throw new RuntimeException("Unknown memIdx: " + memIdx);
			}
		}
		
		private int getMode1Address() {
			switch (memIdx) {
				case 0:
					return getAddr(State.DS_INDEX, (short) (state.getBX() + state.getSI() + nextByte()));
				case 1:
					return getAddr(State.DS_INDEX, (short) (state.getBX() + state.getDI() + nextByte()));
				case 2:
					return getAddr(State.SS_INDEX, (short) (state.getBP() + state.getSI() + nextByte()));
				case 3:
					return getAddr(State.SS_INDEX, (short) (state.getBP() + state.getDI() + nextByte()));
				case 4:
					return getAddr(State.DS_INDEX, (short) (state.getSI() + nextByte()));
				case 5:
					return getAddr(State.DS_INDEX, (short) (state.getDI() + nextByte()));
				case 6:
					return getAddr(State.SS_INDEX, (short) (state.getBP() + nextByte()));
				case 7:
					return getAddr(State.DS_INDEX, (short) (state.getBX() + nextByte()));
				default:
					throw new RuntimeException("Unknown memIdx: " + memIdx);
			}
		}
		
		private int getMode2Address() {
			switch (memIdx) {
				case 0:
					return getAddr(State.DS_INDEX, (short) (state.getBX() + state.getSI() + nextWord()));
				case 1:
					return getAddr(State.DS_INDEX, (short) (state.getBX() + state.getDI() + nextWord()));
				case 2:
					return getAddr(State.SS_INDEX, (short) (state.getBP() + state.getSI() + nextWord()));
				case 3:
					return getAddr(State.SS_INDEX, (short) (state.getBP() + state.getDI() + nextWord()));
				case 4:
					return getAddr(State.DS_INDEX, (short) (state.getSI() + nextWord()));
				case 5:
					return getAddr(State.DS_INDEX, (short) (state.getDI() + nextWord()));
				case 6:
					return getAddr(State.SS_INDEX, (short) (state.getBP() + nextWord()));
				case 7:
					return getAddr(State.DS_INDEX, (short) (state.getBX() + nextWord()));
				default:
					throw new RuntimeException("Unknown memIdx: " + memIdx);
			}
		}
		
		private int getAddr(int segIndex, int offs) {
			if (null != forcedSegIdx) {
				segIndex = forcedSegIdx;
				forcedSegIdx = null;
			}
			return (state.getSegReg(segIndex) << 4) + (offs & 0xffff);
		}
		
		public byte getMem8() {
			if (null == addr) {
				return (byte) state.getReg8(memIdx);
			}
			return mem.getByte(addr);
		}
		
		public void setMem8(byte value) {
			if (null == addr) {
				state.setReg8(memIdx, value);
			} else {
				mem.setByte(addr, value);
			}
		}
		
		public short getMem16() {
			if (null == addr) {
				return (short) state.getReg(memIdx);
			}
			
			return mem.getWord(addr);
		}
		
		public void setMem16(short value) {
			if (null == addr) {
				state.setReg(memIdx, value);
			} else {
				mem.setWord(addr, value);
			}
		}
		
		public byte getReg8() {
			return state.getReg8(regIdx);
		}
		
		public void setReg8(byte value) {
			state.setReg8(regIdx, value);
		}
		
		public int getReg16() {
			return state.getReg(regIdx);
		}
		
		public void setReg16(int value) {
			state.setReg(regIdx, value);
		}
		
		public void setSreg(short value) {
			state.setSegReg(regIdx, value);
		}
		
		public int getSreg() {
			return state.getSegReg(regIdx);
		}
		
		public byte getRegIdx() {
			return regIdx;
		}
		
		public Integer getAddress() {
			return addr;
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
	
		this.mem = mem;
		
		modRM = new ModRM();
		state = new State();
		
		reset();
		
		logger.setUseParentHandlers(false);
		Handler logHandler = new FileHandler("cpu.log");
		logHandler.setFormatter(new LogFormatter());
		logger.addHandler(logHandler);
	}
	
	public void reset() {
		state.reset();
		
		state.setCS(INIT_CS);
		state.setIP(INIT_IP);
		
		state.setFlags(INIT_FLAGS);
	}
	
	public void step() throws Exception {
		byte opcode = nextByte();
		
		logger.info(String.format("%s: 0x%X ", ++opcodeNum, opcode) + state);
		if (opcodeNum > 100000) {
			System.exit(0);
		}
		
		switch (opcode) {
			case (byte) 0x00: // ADD Eb Gb
				modRM.read();
				modRM.setMem8(add8(modRM.getMem8(), modRM.getReg8()));
				break;
			case (byte) 0x01: // ADD Ev Gv
				modRM.read();
				modRM.setMem16(add16(modRM.getMem16(), modRM.getReg16()));
				break;
			case (byte) 0x02: // ADD Gb Eb
				modRM.read();
				modRM.setReg8(add8(modRM.getReg8(), modRM.getMem8()));
				break;
			case (byte) 0x03: // ADD Gv Ev
				modRM.read();
				modRM.setReg16(add16(modRM.getReg16(), modRM.getMem16()));
				break;
			case (byte) 0x04: // ADD AL Ib
				state.setAL(add8(state.getAL(), nextByte()));
				break;
			case (byte) 0x05: // ADD AX Iv
				state.setAX(add16(state.getAX(), nextWord()));
				break;
			case (byte) 0x06: // PUSH ES
				push(state.getES());
				break;
			case (byte) 0x07: // POP ES
				state.setES(pop());
				break;
			case (byte) 0x1E: // PUSH DS
				push(state.getDS());
				break;
			case (byte) 0x1F: // POP DS
				state.setDS(pop());
				break;
			case (byte) 0x20: // AND Eb Gb
				modRM.read();
				modRM.setMem8(and8(modRM.getMem8(), modRM.getReg8()));
				break;
			case (byte) 0x21: // AND Ev Gv
				modRM.read();
				modRM.setMem16(and16(modRM.getMem16(), modRM.getReg16()));
				break;
			case (byte) 0x22: // AND Gb Eb
				modRM.read();
				modRM.setReg8(and8(modRM.getReg8(), modRM.getMem8()));
				break;
			case (byte) 0x23: // AND Gv Ev
				modRM.read();
				modRM.setReg16(and16(modRM.getReg16(), modRM.getMem16()));
				break;
			case (byte) 0x24: // AND AL Ib
				state.setAL(and8(state.getAL(), nextByte()));
				break;
			case (byte) 0x25: // AND AX Iv
				state.setAX(and16(state.getAX(), nextWord()));
				break;
			case (byte) 0x26: // ES:
				modRM.forceSeg(State.ES_INDEX);
				break;
			case (byte) 0x2E: // CS:
				modRM.forceSeg(State.CS_INDEX);
				break;
			case (byte) 0x30: // XOR Eb Gb
				modRM.read();
				modRM.setMem8(xor8(modRM.getMem8(), modRM.getReg8()));
				break;
			case (byte) 0x31: // XOR Ev Gv
				modRM.read();
				modRM.setMem16(xor16(modRM.getMem16(), modRM.getReg16()));
				break;
			case (byte) 0x40: // INC AX
			case (byte) 0x41: // INC CX
			case (byte) 0x42: // INC DX
			case (byte) 0x43: // INC BX
			case (byte) 0x44: // INC SP
			case (byte) 0x45: // INC BP
			case (byte) 0x46: // INC SI
			case (byte) 0x47: // INC DI
				state.setReg(opcode & 0x07, inc16(state.getReg(opcode & 0x07)));
				break;
			case (byte) 0x48: // DEC AX
			case (byte) 0x49: // DEC CX
			case (byte) 0x4A: // DEC DX
			case (byte) 0x4B: // DEC BX
			case (byte) 0x4C: // DEC SP
			case (byte) 0x4D: // DEC BP
			case (byte) 0x4E: // DEC SI
			case (byte) 0x4F: // DEC DI
				state.setReg(opcode & 0x07, dec16(state.getReg(opcode & 0x07)));
				break;
			case (byte) 0x50: // PUSH AX
			case (byte) 0x51: // PUSH CX
			case (byte) 0x52: // PUSH DX
			case (byte) 0x53: // PUSH BX
			case (byte) 0x54: // PUSH SP
			case (byte) 0x55: // PUSH BP
			case (byte) 0x56: // PUSH SI
			case (byte) 0x57: // PUSH DI
				push(state.getReg(opcode & 0x07));
				break;
			case (byte) 0x58: // POP AX
			case (byte) 0x59: // POP CD
			case (byte) 0x5A: // POP DX
			case (byte) 0x5B: // POP BX
			case (byte) 0x5C: // POP SP
			case (byte) 0x5D: // POP BP
			case (byte) 0x5E: // POP SI
			case (byte) 0x5F: // POP DI
				state.setReg(opcode & 0x07, pop());
				break;
			case (byte) 0x72: // JB Jb
				if (state.getCarryFlag()) {
					state.setIP(state.getIP() + nextByte() + 1);
				} else {
					nextByte();
				}
				break;
			case (byte) 0x76: // JBE Jb
				if (state.getCarryFlag() || state.getZeroFlag()) {
					state.setIP(state.getIP() + nextByte() + 1);
				} else {
					nextByte();
				}
				break;
			case (byte) 0x77: // JA Jb
				if (!state.getCarryFlag() && !state.getZeroFlag()) {
					state.setIP(state.getIP() + nextByte() + 1);
				} else {
					nextByte();
				}
				break;
			case (byte) 0x80: // GRP1 Eb Ib
				modRM.read();
				switch (modRM.getRegIdx()) {
					case 4: // AND
						modRM.setMem8(and8(modRM.getMem8(), nextByte()));
						break;
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
			case (byte) 0x8A: // MOV Gb, Eb
				modRM.read();
				modRM.setReg8(modRM.getMem8());
				break;
			case (byte) 0x8B: // MOV Gv Ev
				modRM.read();
				modRM.setReg16(modRM.getMem16());
				break;
			case (byte) 0x8C: // MOV Ew Sw
				modRM.read();
				modRM.setMem16((short)modRM.getSreg());
				break;
			case (byte) 0x8E: // MOV Sw Ew
				modRM.read();
				modRM.setSreg(modRM.getMem16());
				break;
			case (byte) 0xA2: //MOV Ob AL
				mem.setByte((state.getDS() << 4) + (nextWord() & 0xffff), state.getAL());
				break;
			case (byte) 0xA3: //MOV Ov AX
				mem.setWord((state.getDS() << 4) + (nextWord() & 0xffff), (short) state.getAX());
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
			case (byte) 0xB4: // MOV AH Ib
			case (byte) 0xB5: // MOV CH Ib
			case (byte) 0xB6: // MOV DH Ib
			case (byte) 0xB7: // MOV BH Ib
				state.setReg8(opcode & 0x07, nextByte());
				break;
			case (byte) 0xB8: // MOV AX Iv
			case (byte) 0xB9: // MOV CX Iv
			case (byte) 0xBA: // MOV DX Iv
			case (byte) 0xBB: // MOV BX Iv
			case (byte) 0xBC: // MOV SP Iv
			case (byte) 0xBD: // MOV BP Iv
			case (byte) 0xBE: // MOV SI Iv
			case (byte) 0xBF: // MOV DI Iv
				state.setReg(opcode & 0x07, nextWord());
				break;
			case (byte) 0xC3: // RET
				state.setIP(pop());
				break;
			case (byte) 0xC4: // LES Gv Mp
				modRM.read();
				modRM.setReg16(mem.getWord(modRM.getAddress()));
				state.setES(mem.getWord(modRM.getAddress() + 2));
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
				outb(nextByte(), state.getAL());
				break;
			case (byte) 0xEE: // OUT DX AL
				outb((short) state.getDX(), state.getAL());
				break;
			case (byte) 0xE8: // CALL Jv
				push(state.getIP() + 2);
				state.setIP(state.getIP() + nextWord() + 2);
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
				jump = state.getIP() - 1;
				break;
			case (byte) 0xFA: // CLI
				state.setInterruptFlag(false);
				break;
			case (byte) 0xFB: // STI
				state.setInterruptFlag(true);
				break;
			case (byte) 0xFC: // CLD
				state.setDirectionFlag(false);
				break;
			case (byte) 0xFD: // STD
				state.setDirectionFlag(true);
				break;
			case (byte) 0xFE: // GRP4 Eb
				modRM.read();
				switch (modRM.getRegIdx()) {
					case 0: // INC
						modRM.setMem8(inc8(modRM.getMem8()));
						break;
					case 1: // DEC
						modRM.setMem8(dec8(modRM.getMem8()));
						break;
					default: 
						throw new RuntimeException("Invalid regIdx: " + modRM.getRegIdx());
				}
				break;
			case (byte) 0xFF: // GRP5 Ev
				modRM.read();
				switch (modRM.getRegIdx()) {
					case 4: // JMP 
						state.setIP(modRM.getMem16());
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
		
		int diff = (state.getDirectionFlag() ? -1 : 1) << (opcode & 1);
		
		switch (opcode) {
			case (byte) 0xAB: // STOSW
				mem.setWord((state.getES() << 4) + state.getDI(), (short) state.getAX());
				state.setDI(state.getDI() + diff);
				break;
			default:
				throw new InvalidOpcodeException(opcode);
		}
		
		if (jump > 0) {
			state.setCX(state.getCX() - 1);
			if (state.getCX() > 0) {
				state.setIP(jump);
			}
		}
	}

	private byte add8(byte v1, byte v2) {
		short shortResult = (short)((v1 & 0xff) + (v2 & 0xff));
		byte byteResult = (byte) shortResult;
		updateFlags8(shortResult);
		state.setAuxiliaryFlag((v1 & 0xf) + (v2 & 0xf) > 0xf);
		return byteResult;
	}
	
	private short add16(int v1, int v2) {
		updateFlags16((v1 & 0xffff) + (v2 & 0xffff));
		int intRes = (short) v1 + (short) v2;
		state.setOverflowFlag(intRes > 0x7fff || intRes < -0x8000);
		state.setAuxiliaryFlag((v1 & 0xf) + (v2 & 0xf) > 0xf);
		return (short) intRes;
	}
	
	private byte sub8(byte v1, byte v2) {
		updateFlags8((short) ((v1 & 0xff) - (v2 & 0xff)));
		short shortResult = (short) (v1 - v2);
		state.setOverflowFlag(shortResult > 0x7f || shortResult < -0x80);
		state.setAuxiliaryFlag((v1 & 0xf) < (v2 & 0xf));
		return (byte) shortResult;
	}
	
	private short sub16(int v1, int v2) {
		updateFlags16((v1 & 0xffff) - (v2 & 0xffff));
		int intRes = (short) v1 - (short) v2;
		state.setOverflowFlag(intRes > 0x7fff || intRes < -0x8000);
		state.setAuxiliaryFlag((v1 & 0xf) < (v2 & 0xf));
		return (short) intRes;
	}
	
	private byte and8(byte v1, byte v2) {
		short shortResult = (short)((v1 & 0xff) & (v2 & 0xff));
		byte byteResult = (byte) shortResult;
		updateFlags8(shortResult);
		state.setOverflowFlag(false);
		return byteResult;
	}
	
	private short and16(int v1, int v2) {
		int intRes = (v1 & 0xffff) & (v2 & 0xffff);
		short shortRes = (short) intRes;
		updateFlags16(intRes);
		state.setOverflowFlag(false);
		return shortRes;
	}
	
	private byte xor8(byte v1, byte v2) {
		short shortResult = (short) (((short) v1 & 0xff) ^ ((short) v2 & 0xff));
		byte result = (byte) shortResult;
		
		state.setOverflowFlag(false);
		state.setCarryFlag(false);
		state.setAuxiliaryFlag(false); // ??
		updateFlags8(shortResult);
		
		return result;
	}
	
	private short xor16(int v1, int v2) {
		int intRes = (v1 & 0xffff) ^ (v2 & 0xffff);
		short result = (short) intRes;
		
		state.setOverflowFlag(false);
		state.setCarryFlag(false);
		state.setAuxiliaryFlag(false); // ??
		updateFlags16(intRes);
		
		return result;
	}
	
	private byte inc8(byte v) {
		boolean oldCarry = state.getCarryFlag();
		byte result = add8(v, (byte) 1);
		state.setCarryFlag(oldCarry);
		return result;
	}
	
	private short inc16(int v) {
		boolean oldCarry = state.getCarryFlag();
		short result = add16(v, 1);
		state.setCarryFlag(oldCarry);
		return result;
	}
	
	private byte dec8(byte v) {
		boolean oldCarry = state.getCarryFlag();
		byte result = sub8(v, (byte) 1);
		state.setCarryFlag(oldCarry);
		return result;
	}
	
	private short dec16(int v) {
		boolean oldCarry = state.getCarryFlag();
		short result = sub16(v, 1);
		state.setCarryFlag(oldCarry);
		return result;
	}
	
	private short shl16(int v, int count) {
		v <<= count;
		
		updateFlags16(v);
		state.setCarryFlag((v & 0x10000) == 0x10000);
		state.setAuxiliaryFlag((v & 0x10) != 0);
		state.setOverflowFlag(((v >> 16) & 0x1) != ((v >> 15) & 0x1));
		
		return (short) v;
	}
	
	private void push(int value) {
		int sp = (state.getSP() - 2) & 0xffff;
        state.setSP(sp);
        mem.setWord((state.getSS() << 4) + sp, (short) value);
	}
	
	private short pop() {
		short v = mem.getWord((state.getSS() << 4) + state.getSP());
		state.setSP(state.getSP() + 2);
		return v;
	}
	
	private void outb(short port, byte val) {
		// TODO: DMA implementation
		System.out.println(String.format("out 0x%X, 0x%X", port, val));
	}
	
	private void interrupt(byte intNo) {
		push(state.getFlags());
		push(state.getCS());
		push(state.getIP());
		state.setIP(mem.getWord(4 * intNo));
		state.setCS(mem.getWord(4 * intNo + 2));
		System.out.println(String.format("int: 0x%X", intNo));
	}
	
	private void opJmpAp() {
		short newIP = nextWord();
		short newCS = nextWord();
		state.setCS(newCS);
		state.setIP(newIP);
	}
	
	public void updateFlags16(int v) {
		state.setCarryFlag((v & 0xFFFF0000) != 0);
		state.setZeroFlag((short) v == 0);
		
		byte byteVal = (byte) v;
		byte bitSum = 0;
		for (byte b = 1; b != 0; b <<= 1) {
			if ((byteVal & b) != 0) {
				bitSum += 1;
			}
		}
		state.setParityFlag(bitSum % 2 == 0);
		
		state.setSignFlag(((short) v & 0x8000) != 0);
	}
	
	public void updateFlags8(short v) {
		state.setCarryFlag((v & 0xFF00) != 0);
		state.setZeroFlag(v == 0);
		
		byte bitSum = 0;
		for (byte b = 1; b != 0; b <<= 1) {
			if ((v & b) != 0) {
				bitSum += 1;
			}
		}
		state.setParityFlag(bitSum % 2 == 0);

		state.setSignFlag((v & 0x80) != 0);
	}
	
	private byte nextByte() {
		byte result = mem.getByte((state.getCS() << 4) + state.getIP());
		state.setIP(state.getIP() + 1);
		return result;
	}
	
	private short nextWord() {
		short result = mem.getWord((state.getCS() << 4) + state.getIP());
		state.setIP(state.getIP() + 2);
		return result;
	}
}
