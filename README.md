# Is this an okhttp bug?

This project shows what I have observed using okhttp to hit a crate.io blob store.  I used it create 
https://github.com/square/okhttp/issues/3964, which was solved by adding a Connection: close header 
to the request.

--

What appears to be happening, is this:

1. a call to a crate blob that doesn't exist returns a 404
2. the next call, no matter to what returns the same 404 response
3. the next call returns whatever was the response in #2
4. the next call returns whatever was in the response in #3 and so on

It seems from that point on any call the hits the same IP address and uses the same connection from the connection pool
is one response behind.  If I evictAll on the pools after a non 200, then it works correctly.  This is my current 
workaround but it seems wrong. See the OkHttpTest file.  

I have spent way to much time stepping through Interceptors and RealBuffers trying to figure out why this happening but  
have been unable to make this happen with sample servers or understand why this happening.  I suspect it is something 
with the response from crate, but inspecting the headers and trying to replicate did not work.  Hence the instructions 
for starting crate below, either in docker or directly.

I do not see how anything is being cached, because there is no InternalCache on the CacheInterceptor.  I did see
https://github.com/square/okhttp/issues/3020 but when I step through the CacheInterceptor the cacheResponse is null
at https://github.com/square/okhttp/blob/parent-3.6.0/okhttp/src/main/java/okhttp3/internal/cache/CacheInterceptor.java#L144
so it never gets to the isCacheable call on line 147.

I have updated to okhttp to version 3.10.0 but the test results are the same.  This is tested with 3.6.0.

## Setting up crate

### Docker

From the root of this project run this to pull default crate container, start it
and put some blobs in.

```
./setup_crate.sh
```

Then hit http://127.0.0.1:4200/#/tables/blob/myblob to view the blob table.  It should have 3 blobs

To stop the docker instance, run

```
docker stop okhttp-bug-1
```

### Without Docker

Head over to https://crate.io/download/

```
bash -c "$(curl -L try.crate.io)"
```

Now you need to create the blob table and insert some samples.  From the setup_crate.sh script

```
curl -sSPOST  "http://127.0.0.1:4200/_sql?pretty" -d '{"stmt":"create blob table myblob"}' > /dev/null
curl -sSX PUT "http://127.0.0.1:4200/_blobs/myblob/6dcd4ce23d88e2ee9568ba546c007c63d9131c1b" -d "A"
curl -sSX PUT "http://127.0.0.1:4200/_blobs/myblob/32096c2e0eff33d844ee6d675407ace18289357d" -d "C"
curl -sSX PUT "http://127.0.0.1:4200/_blobs/myblob/50c9e8d5fc98727b4bbc93cf5d64a68db647f04f" -d "D"
```

Notice a blob for 'B' was not created.


## Blobs

You should have 3 blobs now, A,C and D.  Documentation on how to insert blobs is at
https://crate.io/docs/crate/reference/en/latest/general/blobs.html#uploading

To get the sha1 content by running something like

```
python -c 'import hashlib;print(hashlib.sha1("A".encode("utf-8")).hexdigest())'
6dcd4ce23d88e2ee9568ba546c007c63d9131c1b
```

So the sha1 values for A,B,C and D are 

|content|sha1                                    | 
|-------|----------------------------------------|
| A     |6dcd4ce23d88e2ee9568ba546c007c63d9131c1b|
| B     |ae4f281df5a5d0ff3cad6371f76d5c29b6d953ec|
| C     |32096c2e0eff33d844ee6d675407ace18289357d|
| D     |50c9e8d5fc98727b4bbc93cf5d64a68db647f04f|

Then you can the responses by running these, as seen on 
https://crate.io/docs/crate/reference/en/latest/general/blobs.html#download

```
curl -sSvL 127.0.0.1:4200/_blobs/myblob/6dcd4ce23d88e2ee9568ba546c007c63d9131c1b
curl -sSvL 127.0.0.1:4200/_blobs/myblob/ae4f281df5a5d0ff3cad6371f76d5c29b6d953ec
curl -sSvL 127.0.0.1:4200/_blobs/myblob/32096c2e0eff33d844ee6d675407ace18289357d
curl -sSvL 127.0.0.1:4200/_blobs/myblob/50c9e8d5fc98727b4bbc93cf5d64a68db647f04f
```