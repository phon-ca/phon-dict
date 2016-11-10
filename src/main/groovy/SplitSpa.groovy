import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

def reader = new CSVReader(
	new InputStreamReader(
		new FileInputStream("src/main/dict/spa/Sadowsky-DIFCAM-Dico--TRIMMED-yr.csv"), "UTF-8"));
	
def dictWriter = new CSVWriter(
	new OutputStreamWriter(
		new FileOutputStream("src/main/dict/spa/Sadowsky-DIFCAM-Dico--TRIMMED--yr-gh.csv"), "UTF-8"), '\t'.charAt(0), '\u0000'.charAt(0), '\u0000'.charAt(0));
	
def phraseWriter = new CSVWriter(
	new OutputStreamWriter(
		new FileOutputStream("src/main/dict/spa/Sadowsky-DIFCAM-Dico--PHRASES-gh.csv"), "UTF-8"), '\t'.charAt(0), '\u0000'.charAt(0), '\u0000'.charAt(0));
	
while((line = reader.readNext()) != null) {
	ortho = line[0];
	if(ortho.indexOf(" ") > 0)
		phraseWriter.writeNext(line);
	else
		dictWriter.writeNext(line);
}

dictWriter.close();
phraseWriter.close();
reader.close();
