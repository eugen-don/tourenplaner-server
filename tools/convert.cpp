#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <limits>
#include <assert.h>
#include <stdint.h>
#include <math.h>

using namespace std;



struct Edge {
    uint32_t sourceId;
    uint32_t targetId;
    uint32_t length;
    uint32_t shortcuttedEdge1;
    uint32_t shortcuttedEdge2;
    
};

struct Node {
    double lon;
    double lat;
    int32_t ele;
    uint32_t rank;
    Edge* outEdges;
};

// PI/180
static const double DEG_TO_RAD = 0.017453292519943295769236907684886;
static const double EARTH_RADIUS_IN_METERS = 6372797.560856;


double calcDirectDistance(double lat1, double lon1, double lat2, double lon2) {
        const double degTorad = 0.017453292519943;
        const double R = 6371;
        const double dLat = (lat2-lat1)*degTorad;
        const double dLon = (lon2-lon1)*degTorad;
        const double a = sin(dLat/2) * sin(dLat/2) + cos(lat1*degTorad) * cos(lat2*degTorad) * sin(dLon/2) * sin(dLon/2);
        const double c = 2 * atan2(sqrt(a), sqrt(1-a));
        return R * c*1000.0;

}

double calcEdgeDistance(Edge* e, Node* nodes){
    return calcDirectDistance(nodes[e->sourceId].lat, nodes[e->sourceId].lon, nodes[e->targetId].lat, nodes[e->targetId].lon);
}


int main(int args, char** argv) {
    bool isCH;
    string ranks;
    string graph;
    ifstream gin;
    ifstream rin;
    // assert endianess
    int x = ('b'<<8)+'a';
    char* y = (char*)&x;
    assert(y[0]=='a' && y[1]=='b');
    // assert type sizes

    graph = args >=2?argv[1]:"graph.dat";
    isCH = args >= 3;
    
    if(isCH)
        ranks = argv[2];

    gin.open(graph.c_str(), ios::in | ios::binary);
    
    if(isCH)
        rin.open(ranks.c_str(), ios::in | ios::binary);
    
    if(!gin.good()) {
        cerr << "Could not open file for reading: "<< graph << endl;
        return 1;
    }
    
    if(isCH && !rin.good()) {
        cerr << "Could not open file reading: "<< ranks << endl;
        return 1;
    }
    
    uint32_t n,nr,ne;
    // Number of nodes read from graph file
    gin.read((char*)&n, sizeof(uint32_t));
    // Number of edges from graph file
    gin.read((char*)&ne, sizeof(uint32_t));
    
    if(isCH){    
    	// Number of nodes read from rank file
    	rin.read((char*)&nr, sizeof(uint32_t));
    
    	if(nr != n){
        	cerr << "Number of nodes in rank file does not match graph file " << n << "vs"<<nr<<endl; 
        	return 1;
    	}
    }


    cerr << "Reading " << n << " nodes and " << ne << " edges"<< endl;
 
    Node* nodes = new Node[n];
    Edge* edges = new Edge[ne];
    // Read nodes
    for(unsigned int i=0 ; i<n; ++i) {        
        gin.read((char*)&nodes[i].lon,sizeof(double));
        gin.read((char*)&nodes[i].lat,sizeof(double));
        gin.read((char*)&nodes[i].ele,sizeof(int32_t));
        if (isCH){
			// Read the rank of the node from rank file
        	rin.read((char*)&nodes[i].rank, sizeof(uint32_t));
		}
    }
    
    // Read edges
    for(unsigned int i=0; i<ne; ++i){
        //gin.read((char*)&edges[i], sizeof(Edge));
        gin.read((char*)&edges[i].sourceId, sizeof(uint32_t));
        gin.read((char*)&edges[i].targetId, sizeof(uint32_t));
        gin.read((char*)&edges[i].length, sizeof(uint32_t));
        gin.read((char*)&edges[i].shortcuttedEdge1, sizeof(uint32_t));
        gin.read((char*)&edges[i].shortcuttedEdge2, sizeof(uint32_t));
    }
    
    // Write num nodes, num edges
    cout << n << endl;
    cout << ne << endl;
    // Write Nodes
    for(unsigned int i=0 ; i<n; ++i) {
       cout << ((int32_t) (nodes[i].lat*10000000.0)) << " ";
       cout << ((int32_t) (nodes[i].lon*10000000.0)) << " ";
       cout << nodes[i].ele;
	   if(isCH){	
           cout << " " <<((nodes[i].rank < (uint32_t)numeric_limits<int32_t>::max())?(int32_t)nodes[i].rank:(int32_t)numeric_limits<int32_t>::max())<< endl;
	   } else{
	   	   cout << endl;
       }
    }
 
    
    // Write Edges
    for(unsigned int i=0 ; i<ne; ++i) {
            cout << edges[i].sourceId << " " ;
            cout << edges[i].targetId << " " ;
            cout << edges[i].length << " ";
            cout << (int) calcEdgeDistance(&edges[i], nodes);
            if(isCH){
                cout << " " << ((edges[i].shortcuttedEdge1< (uint32_t) numeric_limits<int32_t>::max())? (int32_t)edges[i].shortcuttedEdge1 : -1)<< " ";
            	cout << ((edges[i].shortcuttedEdge2 < (uint32_t) numeric_limits<int32_t>::max())? (int32_t)edges[i].shortcuttedEdge2 : -1)<< endl;
		    } else {
		    	cout << endl;
            }
    }
       
    gin.close();
    if(isCH)
    	rin.close();
}

