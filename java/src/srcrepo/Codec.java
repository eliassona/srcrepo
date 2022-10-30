package srcrepo;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Codec {
	public static byte[] encodeBA(final List<List> byteArrays) throws UnsupportedEncodingException {
		int sizeOfData = 0;
		for (final List l: byteArrays) {
			final String name = (String) l.get(0);
			sizeOfData += 1 + name.getBytes(StandardCharsets.UTF_8).length;
			final byte[] b = (byte[]) l.get(1);
			sizeOfData += 4 + b.length;
		}
		final byte ba[] = new byte[1 + sizeOfData];
		ba[0] = (byte) byteArrays.size();
		int i = 1;
		for (final List l: byteArrays) {
			final String name = (String) l.get(0);
			final byte[] nameBa = name.getBytes(StandardCharsets.UTF_8);
			ba[i] = (byte) nameBa.length;
			i++;
			System.arraycopy(nameBa, 0, ba, i, nameBa.length);
			i += nameBa.length;
			final byte[] b = (byte[]) l.get(1);
			encodeBaSize(b.length, i, ba);
			i += 4;
			System.arraycopy(b, 0, ba, i, b.length);
			i += b.length;
		}
		return ba;
	}

	public static byte[] encodeBaSize(final int size, final int i, final byte[] ba) {
		ba[i] = (byte) (size & 255);
		ba[i + 1] = (byte) ((size >> 8) & 255);
		ba[i + 2] = (byte) ((size >> 16) & 255);
		ba[i + 3] = (byte) ((size >> 24) & 255);
		return ba;
	}
	
	public static int decodeBaSize(final byte[] ba, final int i) {
		return ba[i] & 255 | (ba[i + 1] & 255) << 8 | (ba[i + 2] & 255) << 16 | (ba[i + 3] & 255) << 24;
	}
	
	public static List<List> decodeBA(final byte[] ba) {
		final List<List> result = new ArrayList<>();
		final int nrOfBas = ba[0];
		int i = 1;
		for (int j = 0; j < nrOfBas; j++) {
			final List l = new ArrayList();
			result.add(l);
			final int nameSize = ba[i];
			i++;
			final byte[] nameBa = new byte[nameSize];
			System.arraycopy(ba, i, nameBa, 0, nameSize);
			l.add(new String(nameBa, StandardCharsets.UTF_8));
			i += nameSize;
			final int size = decodeBaSize(ba, i);
			final byte[] subBa = new byte[size];
			i += 4;
			System.arraycopy(ba, i, subBa, 0, size);
			i += size;
			l.add(subBa);
		}
		return result;
	}

	public static void main(final String[] args) throws UnsupportedEncodingException {
		byte[] ba = new byte[] {-1, -1, 0, 0};
		System.out.println(decodeBaSize(ba, 0));
//		final List<List> data = Arrays.asList(Arrays.asList("AName", new byte[] {1}));
//		final List<List> dData = decodeBA(encodeBA(data));
//		System.out.println(dData);
	}
	
}
