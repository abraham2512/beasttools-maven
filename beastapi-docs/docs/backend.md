# Akka HTTP Endpoints


### POST /files 
Starts the download for a file if it doesnt exist.

```
curl -X POST http://127.0.0.1:8080/files -d '{"filename": "SafetyDept","filetype": "shapefile","filesource": "/some/path/to/file","filestatus": "start"}'
```
Sample Response
```
Status 201 - Created
{
	"description": "created"
}
```

### GET /files 

Return all the files in our server
```
curl -X GET http://127.0.0.1:8080/files
```
Sample Response
```
Status 200 - OK
{
	"files": [
		{
			"filename": "SafetyDept",
			"filesource": "/some/path/to/file",
			"filestatus": "indexed",
			"filetype": "default"
		},
		{
			"filename": "Sections",
			"filesource": "/some/path/to/file",
			"filestatus": "indexed",
			"filetype": "shapefile"
		}
	]
}
```


### GET /files/{id}  
Returns the details of a file
```
curl -X GET http://127.0.0.1:8080/files/SafetyDept
```
Sample Response
```
Status 200 - OK
{
	"filename": "SafetyDept",
	"filesource": "/some/path/to/file",
	"filestatus": "indexed",
	"filetype": "shapefile"
}
```
### DELETE /files/{id}
Deletes the dataset from server
```
curl -X DELETE http://127.0.0.1:8080/files/SafetyDept
```
Sample Response
```
Status 200 - OK
{
	"description": "deleted"
}
```
### GET /tiles/
Returns pre generated
tile or generates one on the fly.
```
curl -X GET http://127.0.0.1:8080/tiles?dataset=<D>&z=<Z>&x=<X>&y=<Y> 
```
Returns a tile image for the dataset D with coordinates Z, X, Y