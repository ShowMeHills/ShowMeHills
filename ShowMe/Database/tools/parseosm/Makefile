
CPP = g++
CPPFLAGS = -g -Wall
LDFLAGS = -lboost_regex

parseosm: parseosm.cpp
	$(CPP) $(CPPFLAGS) -o parseosm parseosm.cpp  $(LDFLAGS)
	
clean:
	rm -f parseosm parseosm.o