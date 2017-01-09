import json

with open('osm-volcanoes.json') as data_file:
	print data_file
	data = json.load(data_file)

op = open('volcanoes.csv',"w")

for rec in data['elements']:
	try:
		t = rec['tags']['name']
	except:
		rec['tags']['name'] = 'volcano'
	try:
		t = rec['tags']['ele']
	except:
		rec['tags']['ele'] = 0
	try:
		if not rec['tags']['ele'].isdigit():
			print rec['tags']['ele']
	except:
		pass
	txt = "insert into mountains (webid, latitude, longitude, height, name, linktype) values ('%d',%f,%f,%s,'%s',3);\n" % (rec['id'], rec['lat'], rec['lon'], rec['tags']['ele'], rec['tags']['name'])
	op.write(txt.encode('utf8'))