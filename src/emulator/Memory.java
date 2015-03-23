package emulator;

public class Memory {

    private byte[] mem;
    
    public Memory(int size) {
        mem = new byte[size];
    }
    
    public void loadData(int addr, byte[] data) {
        System.arraycopy(data, 0, mem, addr, data.length);
    }
    
    public byte getByte(int addr) {
        return mem[addr];
    }
    
    public short getWord(int addr) {
        return (short) ((mem[addr] & 0xff) | (mem[addr + 1] << 8));
    }
    
    public void setByte(int addr, byte v) {
        mem[addr] = v;
    }
    
    public void setWord(int addr, short v) {
        mem[addr] = (byte)v;
        mem[addr + 1] = (byte)(v >> 8);
    }
}
