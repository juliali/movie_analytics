an example:
python classifer.py --test-file=data/movie_1_test.arff --train-file=data/movie_1.arff


The following query is used to create mtime_revenue_3 based on movie and movie_revenue_china tables:
create view mtime_revenue_3 as select m.Id as id, m.chinese_name as chinese_name, m.director as director, m.starring as starring, m.type as type, m.rate as rate, m.votes as votes, m.region as region, m.runtime as runtime, m.certification as certification, m.language as language, m.company as company, (m.release_date) as release_date,  mr.box_office_revenue as revenue from movie m, movie_revenue_china mr where m.release_date is not NULL and m.chinese_name is not NULL and m.chinese_name <> '' and m.chinese_name = mr.chinese_name and mr.box_office_revenue > 0 group by m.chinese_name;
