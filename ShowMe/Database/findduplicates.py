import MySQLdb as mdb
import sys


con = None

con = mdb.connect('localhost', 'root', '', 'showmehills')

cur = con.cursor()
cur.execute("SELECT * from hills where hills_source like 'British Hills Db%' and hills_type = 'peak' ")
rows = cur.fetchall()
gap = 0.05
for row in rows:
	cur.execute( "select * from hills where hills_longitude between %f and %f and hills_latitude between %f and %f and hills_type = 'peak' and hills_source like 'OSM%%'" % ( row[2]-gap, row[2]+gap, row[3]-gap, row[3]+gap))
	hills = cur.fetchall()
	if len(hills) > 1:
		for hill in hills:
			if row[1] == hill[1]:
				print "%s (%f,%f) %s" % (row[1], row[2], row[3], row[7])
				print "%s (%f,%f) %s" % (hill[1], hill[2], hill[3], hill[7])
				print "update hills set hills_tobeused=1 where id = %d" % (hill[0]) 
				cur.execute("update hills set hills_tobeused=1 where id = %d" % (hill[0]) )
				con.commit()
		print 
if con:
	con.close()
