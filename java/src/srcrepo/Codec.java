package srcrepo;

import java.util.ArrayList;
import java.util.List;

public class Codec {
	public static byte[] encodeBA(final List<List> byteArrays) {
		int sizeOfData = 0;
		for (final List l: byteArrays) {
			final byte[] b = (byte[]) l.get(1);
			sizeOfData += b.length;
		}
		final byte ba[] = new byte[1 + byteArrays.size() * 4 + sizeOfData];
		ba[0] = (byte) byteArrays.size();
		int i = 1;
		for (final List l: byteArrays) {
			final byte[] b = (byte[]) l.get(1);
			encodeBaSize(b.length, i, ba);
			i += 4;
			System.arraycopy(b, 0, ba, i, b.length);
			i += b.length;
		}
		return ba;
	}

	public static void encodeBaSize(final int size, final int i, final byte[] ba) {
		ba[i] = (byte) (size & 255);
		ba[i + 1] = (byte) ((size >> 8) & 255);
		ba[i + 2] = (byte) ((size >> 16) & 255);
		ba[i + 3] = (byte) ((size >> 24) & 255);
	}
	
	private static int decodeBaSize(final byte[] ba, final int i) {
		return ba[i] | ba[i + 1] << 8 | ba[i + 2] << 16 | ba[i + 3] << 24;
	}
	
	public static List<byte[]> decodeBA(final byte[] ba) {
		final List<byte[]> result = new ArrayList<>();
		final int nrOfBas = ba[0];
		int i = 1;
		for (int j = 0; j < nrOfBas; j++) {
			final int size = decodeBaSize(ba, i);
			final byte[] subBa = new byte[size];
			i += 4;
			System.arraycopy(ba, i, subBa, 0, size);
			i += size;
			result.add(subBa);
		}
		return result;
	}

	public static void main(final String[] args) {
//		final List<byte[]> data = Arrays.asList(new byte[] {1});
//		final List<byte[]> dData = decodeBA(encodeBA(Arrays.asList(new byte[] {1})));
//		System.out.println(dData);
	}
	
}
