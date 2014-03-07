an example:
python classifer.py --test-file=data/movie_1_test.arff --train-file=data/movie_1.arff


The following query is used to create mtime_revenue_3 based on movie and movie_revenue_china tables:
create VIEW mtime_revenue_6 AS
SELECT m.Id AS id,
       m.chinese_name AS chinese_name,
       SUBSTRING_INDEX(m.director, ';', 1) AS director1,
       SUBSTRING_INDEX(SUBSTRING_INDEX(m.director, ';', 2), ';', -1) AS director2,       
       SUBSTRING_INDEX(m.starring, ';', 1) AS starring1,
       SUBSTRING_INDEX(SUBSTRING_INDEX(m.starring, ';', 2), ';', -1) AS starring2,
       SUBSTRING_INDEX(SUBSTRING_INDEX(m.starring, ';', 3), ';', -1) AS starring3,
       SUBSTRING_INDEX(SUBSTRING_INDEX(m.starring, ';', 4), ';', -1) AS starring4,
       SUBSTRING_INDEX(SUBSTRING_INDEX(m.starring, ';', 5), ';', -1) AS starring5,
       m.type AS TYPE,
       m.rate AS rate,
       m.votes AS votes,
       m.region AS region,
       m.runtime AS runtime,
       m.certification AS certification,
       m.language AS LANGUAGE,
       m.company AS company,
       (m.release_date) AS release_date,
       mr.box_office_revenue AS revenue
FROM movie_copy m,
     movie_revenue_58921 mr
WHERE m.release_date IS NOT NULL
  AND m.chinese_name IS NOT NULL
  AND m.chinese_name <> ''
  AND m.chinese_name = mr.chinese_name
  AND mr.box_office_revenue > 0
GROUP BY m.chinese_name;

