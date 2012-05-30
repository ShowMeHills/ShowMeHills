import csv
import re, string

# script to take csv files which have been created individually and 
# concatenate them together, add an index value, and also make a few
# checks on the values

removeLetters = re.compile('[a-zA-Z ~\+]+')

def convertStr(s):
	"""Convert string to either int or float."""
	try:
		ret = int(s)
	except ValueError:
		#Try float.
		ret = float(s)

	return ret

regions = [ 'Europe', 'Americas']
csvfiletypes = [ 'peaks', 'historic', 'places' ]
of = open("out.csv", 'w')
csv_writer = csv.writer(of, delimiter=',', quotechar='"', quoting=csv.QUOTE_NONNUMERIC)
idx = 0
for region in regions:
	for filety in csvfiletypes:
		filename = "../../" + region + "/" + region.lower() + ".osm_" + filety + ".csv"
		print "doing ", filename
		infile = open(filename)
		f = csv.reader(infile, delimiter=',', quotechar='"')
		for line in f:		
			try:
				# convert lon & lat strings to floats
				line[2] = convertStr( line[2] )
				line[3] = convertStr( line[3] )	
				# remove odd characters from altitude
				if line[4] != '':
					line[4] = re.sub(removeLetters, '', line[4])
					line[4] = convertStr( line[4] )
				idx = idx + 1
				line.insert(0, idx)			
				csv_writer.writerow(line)
			
			except:
				print line
				try:
					print line[4] + ","+  line[5] + "\n"
				except:
					pass
		infile.close()

of.close()
	