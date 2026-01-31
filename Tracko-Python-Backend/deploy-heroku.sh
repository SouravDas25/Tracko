heroku config:set DISABLE_COLLECTSTATIC=1
heroku git:remote -a tracko-nlp
git add .
git commit -m "preparing for heroku push"
git push heroku master