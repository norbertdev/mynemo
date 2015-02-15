Objective
=========

Mynemo suggests your next movie to see. For now, Mynemo use a data set of ratings and your personal ratings. These ratings are imported from [MovieLens](http://classic.movielens.org/).


How to start Mynemo?
====================


Download ratings
----------------

Mynemo needs your personal ratings and a data set containing plenty of ratings.

* A data set is provided by [GroupLens](http://grouplens.org/datasets/movielens/):
```sh
wget http://files.grouplens.org/datasets/movielens/ml-10m.zip
unzip ml-10m.zip
```
The ratings are in the *ratings.dat* file.

* The previous data set contains only the MovieLens ids of the movies. In order to convert them to the IMDb ids, a file containing the necessary data must be provided. This file can be generated from a connected session on the classic MovieLens web site. Perform an advanced search restricted to movies found in any of all selected genres, then download the result. The resulting file is about 1.5 MB.

* Your personal ratings can be exported from the classic MovieLens web site by displaying them, then downloading the result. 


Build Mynemo
------------

* Clone with [git](http://git-scm.com/) and build with [Maven](http://maven.apache.org/):
```sh
git clone http://github.com/norbertdev/mynemo
cd mynemo
mvn package
```


Start Mynemo
------------

Before obtaining a recommendation, the data set must be converted and merged with  your ratings. Then the better algorithm for you must be selected. Finally the recommendations can be produced. Three commands are provided for these steps.

* Convert and merge the ratings:
```sh
target/appassembler/bin/import  --out mynemo-dataset.tsv  --in my-ratings.txt ratings.dat  --movies movies.txt
```
In the preceding example, *my-ratings.txt* is your ratings exported from the MovieLens web site, *ratings.dat* is the file from the MovieLens data set and *movies.txt* the file generated from the MovieLens web site. Other options can be given to the command. Run the command without any option to view the usage.

* Select the best algorithm for you:
```sh
target/appassembler/bin/select  --data-model mynemo-dataset.tsv  --user 2147483647
```
In the preceding example, *2147483647* is the default user id given to your ratings. Other options can be given to the command. Run the command without any option to view the usage. The selection process ends by giving the options to provide to the last command.

* Produce the recommendations:
```sh
target/appassembler/bin/recommend  --algorithm USER_SIMILARITY_WITH_EUCLIDEAN_DISTANCE  --data-model mynemo-dataset.tsv  --user 2147483647  --neighbors 1398
```
In the preceding example, the options used were provided by the *select* command. Other options can be given to the command, like the number of recommendations to generate. Run the command without any option to view the usage. The command ends by giving an ordered list of recommendations, with there associated rating predictions.


License
=======

[Apache License version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).
