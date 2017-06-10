# A spatial Test using lucene
## to build index

curl http://localhost:8080/build

## to continuously build index

curl http://localhost:8080/loop

## to search using circle

curl http://localhost:8080/searchcircle?lat=103.835382&lon=1.3&radius=10

## to search using bounding box

curl http://localhost:8080/searchbbox?minlat=103.835882&minlon=1.3&maxlat=104.835882&maxlon=1.4

## to search using polygon

curl -XPOST 'http://localhost:8080/searchpolygon'  -H "Content-Type: application/json" -d '{
    "polygon":
        [
            {
                "lat":103.823810,
                "lon":1.310760
            },
            {
                "lat":103.824081,
                "lon":1.332759
            },
            {
                "lat":103.841371,
                "lon":1.332272
            },
            {
                "lat":103.852427,
                "lon":1.307022
            },
            {
                "lat":103.832374,
                "lon":1.308105
            },
            {
                "lat":103.823810,
                "lon":1.310760
            }
        ]
}'

## Performance

Total Transaction records	3000,000
Total Unique Key	1896254
Total JVM memory usage	1181 MB
Insert Speed	40k/s
