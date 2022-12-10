import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 *
 * Revise
 */

public class HuffProcessor {
private String poop; 
private class HuffNode implements Comparable<HuffNode> {
HuffNode left;
HuffNode right;
int value;
int weight;

public HuffNode(int val, int count) {
value = val;
weight = count;
}
public HuffNode(int val, int count, HuffNode ltree, HuffNode rtree) {
value = val;
weight = count;
left = ltree;
right = rtree;
}

public int compareTo(HuffNode o) {
return weight - o.weight;
}
}

public static final int BITS_PER_WORD = 8;
public static final int BITS_PER_INT = 32;
public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
public static final int PSEUDO_EOF = ALPH_SIZE;
public static final int HUFF_NUMBER = 0xface8200;
public static final int HUFF_TREE  = HUFF_NUMBER | 1;

private boolean myDebugging = false;

public HuffProcessor() {
this(false);
}

public HuffProcessor(boolean debug) {
myDebugging = debug;
}

/**
* Compresses a file. Process must be reversible and loss-less.
*
* @param in
*            Buffered bit stream of the file to be compressed.
* @param out
*            Buffered bit stream writing to the output file.
*/
public void compress(BitInputStream in, BitOutputStream out){
int[] counts = getCounts(in); 
HuffNode root = makeTree(counts); 
in.reset();
out.writeBits(BITS_PER_INT,HUFF_TREE); 
writeTree(root, out); 
String[] encodings = new String[ALPH_SIZE+1];
makeEncodings(root,"",encodings); 

in.reset(); 


while(true){
int idk = in.readBits(BITS_PER_WORD);
if (idk == -1){
break;
}
else{
String code = encodings[idk];
out.writeBits(code.length(),Integer.parseInt(code,2)); 
}

}
String code = encodings[PSEUDO_EOF];
out.writeBits(code.length(), Integer.parseInt(code,2)); 
out.close();
}


private int[] getCounts(BitInputStream in){
int[] counts = new int[ALPH_SIZE];
int reader = in.readBits(BITS_PER_WORD);

for (int i : counts){
counts[i] = 0; 
}

while (true){
counts[reader]++;
reader = in.readBits(BITS_PER_WORD);
if (reader == -1) break; 

}
return counts; 
}

private HuffNode makeTree(int[] counts){
PriorityQueue<HuffNode> pq = new PriorityQueue<>(); 
for (int i = 0; i < counts.length; i++){
if (counts[i] > 0){
pq.add(new HuffNode(i, counts[i], null, null));
}
}
pq.add(new HuffNode(PSEUDO_EOF,1,null,null));

while (pq.size() > 1 ){
HuffNode left = pq.remove();
  HuffNode right = pq.remove();
HuffNode t = new HuffNode(0, left.weight+right.weight, left, right); 
pq.add(t); 
}

HuffNode root = pq.remove();
return root; 
}

private String[] makeEncodings(HuffNode root, String path, String[] encodings){
if (root == null ){
return encodings; 
}

if (root.left == null && root.right == null){
encodings[root.value] = path; 
}

makeEncodings(root.left, path+"0", encodings); 
makeEncodings(root.right, path+"1", encodings); 
return encodings; 

}

private void writeTree(HuffNode root, BitOutputStream out){

if (root == null){
return; 
}

if (root.left == null && root.right == null){
out.writeBits(1,1);
out.writeBits(BITS_PER_WORD+1, root.value);
}
else {
out.writeBits(1,0);
}
writeTree(root.left, out);
writeTree(root.right,out); 
}


/**
* Decompresses a file. Output file must be identical bit-by-bit to the
* original.
*
* @param in
*            Buffered bit stream of the file to be decompressed.
* @param out
*            Buffered bit stream writing to the output file.
* @throws Exception
*/
public void decompress(BitInputStream in, BitOutputStream out){

int bits = in.readBits(BITS_PER_INT); 
if (bits != HUFF_TREE){
throw new HuffException("invalid magic number " + bits); 
}

HuffNode root = readTree(in); 
HuffNode current = root; 
while (true) {
bits = in.readBits(1);
if (bits == -1){
throw new HuffException("bad input, no PSEUDO_EOF");
}
else {
if (bits == 0 ) {
current = current.left; 
}
else {
current = current.right; 
}

if (current.left == null && current.right == null){
if (current.value == PSEUDO_EOF){
break; 
}
else {
out.writeBits(BITS_PER_WORD,current.value);
current = root; 
}
}
}
}
out.close(); 
}



private HuffNode readTree(BitInputStream in) {
int bit = in.readBits(1); 
if (bit == -1) { throw new HuffException("text"); }

if (bit == 0) {
HuffNode left = readTree(in);
HuffNode right = readTree(in); 
return new HuffNode(0,0,left,right);
}
else {
int value = in.readBits(BITS_PER_WORD +1); 
return new HuffNode(value,0, null, null); 
}
}

}
