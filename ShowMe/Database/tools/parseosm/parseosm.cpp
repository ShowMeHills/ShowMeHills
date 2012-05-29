/*

Quick and dirty file to extract longitude and latitude values from openstreetmap (osm) files.

The osm files are downloaded from http://downloads.cloudmade.com/ and are generally huge.
Usually they are named like;
europe.osm.bz2.part.00 (3814.7M)
europe.osm.bz2.part.01 (3814.7M)
europe.osm.bz2.part.02 (2147.3M) 

so need to concat files together before gunzip2'ing them (on linux that's cat file1 file2 file3 > op.osm.bz2  with part.00 first)

once concatenated & unzipped you end up with one humongous file that too big for fstream to handle well, so use split 
to break up again, ie;

split -b 1G europe.osm

the default names is xaa, xab, xac etc, which gives the stupid array below. I could make this neater obv...

So to parse the files do;

parseosm europe.osm

The parameter is merely used to create the output filenames (initially it opened the osm file), so this could be neater too...

Nik

*/

// reading a text file
#include <iostream>
#include <fstream>
#include <string>
#include <algorithm> 
#include <functional> 
#include <locale>

#include <boost/regex.hpp>

using namespace std;
//g++ -o parseosm parseosm.cpp -lboost_regex


static inline std::string &ltrim(std::string &s) {
        s.erase(s.begin(), std::find_if(s.begin(), s.end(), std::not1(std::ptr_fun<int, int>(std::isspace))));
        return s;
}

#define _CHECK_( _outputfile_, _regex_ ) 	\
if (boost::regex_search(line, _regex_)) \
	{ \
	  if (regex_search(line, nameres, nameregex) && regex_search(line, latres, latregex) && regex_search(line, lonres, lonregex)) \
	  { \
		_outputfile_ <<  #_regex_ << ",\""<< nameres[1] << "\"," << latres[1] <<  "," <<  lonres[1]<<  ","; \
		if (regex_search(line, eleres, eleregex)) \
		{ \
			string str = eleres[1]; \
			std::replace( str.begin(), str.end(), ',', '.' ); \
			_outputfile_ << str; \
		} \
		_outputfile_ << endl; \
	  } \
	} \
	
int main (int argc, const char* argv[] ) {
	  string line;
	string tmpline;
	
	string peaksfilename = argv[1];
	peaksfilename.append("_peaks.csv");
	ofstream peakoutfile(peaksfilename.c_str());
	
	string placesfilename = argv[1];
	placesfilename.append("_places.csv");
	ofstream placeoutfile(placesfilename.c_str());
	
	string historicfilename = argv[1];
	historicfilename.append("_historic.csv");
	ofstream historicpeakoutfile(historicfilename.c_str());
	
	
	// natural tags
	/*
	peak
	*/
	string peakex = "<tag k=\"natural\" v=\"peak";
	boost::regex peak(peakex);
	
	// place tags
	/*
	city
	village
	town
	hamlet
	*/
	
	string cityex = "<tag k=\"place\" v=\"city";
	boost::regex city(cityex);
	string villageex = "<tag k=\"place\" v=\"village";
	boost::regex village(villageex);
	string townex = "<tag k=\"place\" v=\"town";
	boost::regex town(townex);
	string hamletex = "<tag k=\"place\" v=\"hamlet";
	boost::regex hamlet(hamletex);
	
	// historic tags
	/*
	castle
	ruins
	
	*/
	string castleex = "<tag k=\"historic\" v=\"castle";
	boost::regex castle(castleex);
	string ruinsex = "<tag k=\"historic\" v=\"ruins";
	boost::regex ruins(ruinsex);

	string name = "<tag k=\"name\" v=\"(.*?)\"/>";
	string lon = "lon=\"(.*?)\"";
	string lat = "lat=\"(.*?)\"";
	string ele = "<tag k=\"ele\" v=\"(.*?)\"";
	
	boost::regex nameregex(name);
	boost::regex lonregex(lon);
	boost::regex latregex(lat);
	boost::regex eleregex(ele);
	
	boost::smatch nameres;
	boost::smatch lonres;
	boost::smatch latres;
	boost::smatch eleres;
	cout << "preinit";
	string filenames[] = {"xaa","xab","xac","xad","xae","xaf","xag","xah","xai","xaj","xak","xal","xam","xan","xao",
"xap","xaq","xar","xas","xat","xau","xav","xaw","xax","xay","xaz","xba","xbb",
"xbc","xbd","xbe","xbf","xbg","xbh","xbi","xbj","xbk","xbl","xbm","xbn","xbo",
"xbp","xbq","xbr","xbs","xbt","xbu","xbv","xbw","xbx","xby","xbz","xca","xcb",
"xcc","xcd","xce","xcf","xcg","xch","xci","xcj","xck","xcl","xcm","xcn","xco",
"xcp","xcq","xcr","xcs","xct","xcu","xcv","xcw","xcx","xcy","xcz","xda","xdb",
"xdc","xdd","xde","xdf","xdg","xdh","xdi","xdj","xdk","xdl","xdm","xdn","xdo",
"xdp","xdq","xdr","xds","xdt","xdu","xdv","xdw","xdx","xdy","xdz","xea","xeb",
"xec","xed","xee","xef","xeg","xeh","xei","xej","xek","xel","xem","xen","xeo",
"xep","xeq","xer","xes","xet","xeu","xev","xew","xex","xey","xez","xfa","xfb"};

int loops = 0;
	int index = 0;
	int maxf =sizeof filenames/sizeof(filenames[0]);
cout << "array is " << maxf << endl;
	ifstream myfile (filenames[0].c_str());
  if (myfile.is_open())
  {
    while ( myfile.good() )
    {
	    bool startnode = false;
	    bool endnode = false;
	    line = "";
	    while (!startnode)
	    {
		if (myfile.eof()) 
		{
			myfile.close();
			cout << "done " << filenames[index] << endl;
			index++;
			if (index > maxf)  goto finishup;
			myfile.open(filenames[index].c_str());
			if (!myfile.is_open()) goto finishup;
		}
		getline (myfile,line) ;
		startnode = (ltrim(line).compare(0, 5, "<node")  == 0);
		loops = 0;
	    }
	    while (!endnode)
	    {
		    if (myfile.eof()) 
			{
				myfile.close();
				cout << "done " << filenames[index] << endl;
				index++;
				if (index > maxf)  goto finishup;
				myfile.open(filenames[index].c_str());
				if (!myfile.is_open()) goto finishup;
				string tmptmpline;
				getline(myfile, tmptmpline) ;
				tmpline += tmptmpline;
			}
			else
			{
				getline(myfile, tmpline) ;
			}
		    if (	ltrim(tmpline).compare(0, 5, "<node")  == 0 || 
				ltrim(tmpline).compare(0, 4, "<way")  == 0 ||
				ltrim(tmpline).compare(0, 4, "<rel")  == 0 ||
				ltrim(tmpline).compare(0, 5, "</rel")  == 0 ||
				ltrim(tmpline).compare(0, 5, "</way") == 0)
		    {
			    line = "";
			    loops = 0;
		    }
		    else if (ltrim(tmpline).compare(0, 4, "<nd ")  == 0 ||
			        ltrim(tmpline).compare(0, 4, "<mem")  == 0)
		    {
		    }
		    else
		    {
			    line.append(tmpline);
			    loops++;
		    }
		    if (loops > 1000) {
			    // somthing gone wrong]
			    cout << "error: ";
			    cout << line << endl;
			    goto finishup;
		    }
		    endnode = (ltrim(tmpline).compare(0, 6, "</node")  == 0);
	    }	 
	    
	_CHECK_( peakoutfile, peak )
	
	_CHECK_( placeoutfile, city )
	_CHECK_( placeoutfile, village )
	_CHECK_( placeoutfile, town )
	_CHECK_( placeoutfile, hamlet )
	    
	_CHECK_( historicpeakoutfile, castle )
	_CHECK_( historicpeakoutfile, ruins )
	    /*
	if (boost::regex_search(line, peak))
	{
	  if (regex_search(line, nameres, nameregex) && regex_search(line, latres, latregex) && regex_search(line, lonres, lonregex))
	  {
		peakoutfile << nameres[1] << "," << latres[1] <<  "," <<  lonres[1]<<  ",";
		if (regex_search(line, eleres, eleregex))
		{
			peakoutfile << eleres[1];
		}
		peakoutfile << endl;
	  }
	}*/

    }
finishup:
    peakoutfile.close();
    placeoutfile.close();
    historicpeakoutfile.close();
    myfile.close();
  }

}