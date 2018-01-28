## Shallow text summarization methods implementation.

## How to build
---------------
In order to build this project you need to have maven installed on your system.

After cloning run these commands:
```bash
$ mvn install:install-file -Dfile=lib/nnkstemmer.jar -DartifactId=nnkstemmer -DgroupId=nnkstemmer -Dversion=1.0 -Dpackaging=jar 
```
And then:
```bash
$ mvn -Prelease clean install
```

## How to run
--------------
In order to run this program you need to have a collection of documents. Currently only greek documents are supported.
Put the documents in a directory of your preference and run the program passing the ```directory```
argument pointing to the aforementioned directory.  
The program will index those documents and generate a summary
for each of them.
Example
```bash
$ java -jar target/summarization.jar directory <your>/<dir>/<here>
```
After the execution you should see a **summaries** directory inside your current directory.  
This is where the summaries are stored as text files.