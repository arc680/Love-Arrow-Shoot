package info.arc680.lovearrowshoot;

import android.app.Activity;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.image.SmartImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import twitter4j.DirectMessage;
import twitter4j.ResponseList;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.UserStreamAdapter;
import twitter4j.UserStreamListener;
import twitter4j.auth.AccessToken;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationContext;


public class MainActivity extends ActionBarActivity {
    private ListView listView;
    private TweetAdapter mAdapter;
    private Twitter mTwitter;
    private TwitterStream mStream;
    private /*static */Handler mHandler;// = new Handler();

    private SmartImageView iconSiv;
    private TextView nameTv;
    private TextView screenNameTv;
    //private TextView tweetTv;
    private TextView viaTv;
    private TextView dateTv;
    private TextView retweetLabelTv;
    private SmartImageView retweetUserIconSiv;
    private TextView retweetUserTv;

    private ImageButton tweetBtn;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView)findViewById(R.id.timeline);
        mHandler = new Handler();

        if (!TwitterUtils.hasAccessToken(this)) {
            Intent intent = new Intent(this, TwitterOAuthActivity.class);
            startActivity(intent);
            finish();
        } else {
            mAdapter = new TweetAdapter(this);
            listView.setAdapter(mAdapter);

            // ゆーざーすとりーむが向こう
            //reloadTimeLine();

            // ゆーざーすとりーむがゆうこう
            mStream = TwitterUtils.getTwitterStream(getApplicationContext());
            mStream.addListener(new MyStreamAdapter());

            // UserStream開始
            mStream.user();

            tweetBtn = (ImageButton)findViewById(R.id.tweet_button);
            tweetBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this, TweetActivity.class);
                    startActivity(intent);

                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // TODO これだとあらかじめ用意した画像でないとダメ…
        MenuItem menuMyIcon = (MenuItem) menu.findItem(R.id.myIcon);
        if (menuMyIcon.isEnabled()) {
            //menuMyIcon.setIcon(android.R.drawable.ic_lock_silent_mode_off);
        }/* else {
            menu_bgm.setIcon(android.R.drawable.ic_lock_silent_mode);
        }*/

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_tweet) {
            Intent intent = new Intent(this, TweetActivity.class);
            startActivity(intent);

            return true;
        }
        if (id == R.id.myIcon) {
            return true;
        }
        /*if (id == R.id.action_refresh) {
            reloadTimeLine();

            return true;
        }*/
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // userstream はこれ経由で、TweetAdapter へツイートを渡す
    private class MyStreamAdapter extends UserStreamAdapter {
        @Override
        public void onStatus(final Status status) {

            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    //showToast(status.getText());
                    mAdapter.insert(status, 0);
                    mAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private class TweetAdapter extends ArrayAdapter<twitter4j.Status> {
        private LayoutInflater mInflater;

        public TweetAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1);
            mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item_tweet, null);
            }
            Status item = getItem(position);

            // TODO コンパクトにしよう
            // TODO item に retweet されたものを突っ込んで、元のを新しいインスタンスに入れる？
            if (item.isRetweet()) {
                Status retweetItem = getItem(position); // 新しい変数に元を入れる
                item = retweetItem.getRetweetedStatus();   // retweet したのを入れる

                TextView retweetLabel = (TextView) convertView.findViewById(R.id.retweet_label);
                retweetLabel.setVisibility(View.VISIBLE);
                SmartImageView retweetUserIcon = (SmartImageView) convertView.findViewById(R.id.retweet_user_icon);
                retweetUserIcon.setImageUrl(retweetItem.getUser().getProfileImageURL());
                retweetUserIcon.setVisibility(View.VISIBLE);
                TextView retweetUser = (TextView) convertView.findViewById(R.id.retweet_user);
                retweetUser.setText("Retweeted by " + retweetItem.getUser().getScreenName());
                retweetUser.setVisibility(View.VISIBLE);
            } else {
                // retweet じゃないのでもろもろ非表示
                TextView retweetLabel = (TextView) convertView.findViewById(R.id.retweet_label);
                retweetLabel.setVisibility(View.GONE);
                SmartImageView retweetUserIcon = (SmartImageView) convertView.findViewById(R.id.retweet_user_icon);
                retweetUserIcon.setVisibility(View.GONE);
                TextView retweetUser = (TextView) convertView.findViewById(R.id.retweet_user);
                retweetUser.setVisibility(View.GONE);
            }

            SmartImageView icon = (SmartImageView) convertView.findViewById(R.id.icon);
            icon.setImageUrl(item.getUser().getProfileImageURL());
            TextView name = (TextView) convertView.findViewById(R.id.name);
            name.setText(item.getUser().getName());
            TextView screenName = (TextView) convertView.findViewById(R.id.screen_name);
            screenName.setText("@" + item.getUser().getScreenName());
            TextView text = (TextView) convertView.findViewById(R.id.text);
            text.setText(item.getText());
            TextView via = (TextView) convertView.findViewById(R.id.via);
            via.setText(item.getSource().replaceAll("<.+?>", ""));
            TextView date = (TextView) convertView.findViewById(R.id.date);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            date.setText(sdf.format(item.getCreatedAt()));

            return convertView;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mStream.cleanUp();
    }

    private void reloadTimeLine() {
        AsyncTask<Void, Void, List<twitter4j.Status>> task = new AsyncTask<Void, Void, List<twitter4j.Status>>() {
            @Override
            protected List<twitter4j.Status> doInBackground(Void... params) {
                try {
                    return mTwitter.getHomeTimeline();
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(List<twitter4j.Status> result) {
                if (result != null) {
                    mAdapter.clear();
                    for (twitter4j.Status status : result) {
                        mAdapter.add(status);
                    }
                } else {
                    showToast("タイムラインの取得に失敗しました。。。");
                }
            }
        };
        task.execute();
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
