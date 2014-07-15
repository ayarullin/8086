package emulator;

public class InvalidOpcodeException extends Exception {

	private static final long serialVersionUID = -7756307905137172293L;

	public InvalidOpcodeException(byte opcode) {
		super(String.format("0x%X", opcode));
	}
}
