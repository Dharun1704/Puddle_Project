package com.example.puddle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.puddle.NewsModel.Article;
import com.example.puddle.NewsModel.Source;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Objects;

public class NewsDetailActivity extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener{

    private FrameLayout dateLayout;
    private LinearLayout appbarTitle;
    private WebView webView;
    private NewsDatabase db;

    ImageView imageView;
    TextView title, appbar_title, appbar_subTitle, date, time;
    AppBarLayout appBarLayout;
    Toolbar toolbar;

    ArrayList<Article> articles;
    String mUrl, mImage, mTitle, mDate, mSource, mAuthor, mDesc;
    private boolean hideToolbar = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);

        webView = findViewById(R.id.webView);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getColor(R.color.colorAccent));

        final CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle("");

        db = new NewsDatabase(this);
        articles = new ArrayList<>();

        appBarLayout = findViewById(R.id.appbar);
        appBarLayout.addOnOffsetChangedListener(this);
        dateLayout = findViewById(R.id.date_behavior);
        appbarTitle = findViewById(R.id.title_appbar);
        imageView = findViewById(R.id.backdrop);
        appbar_title = findViewById(R.id.title_on_appbar);
        appbar_subTitle = findViewById(R.id.subtitle_on_appbar);
        title = findViewById(R.id.title);
        time = findViewById(R.id.time);
        date = findViewById(R.id.date);

        Intent intent = getIntent();
        mUrl = intent.getStringExtra("url");
        mImage = intent.getStringExtra("img");
        mTitle = intent.getStringExtra("title");
        mDate = intent.getStringExtra("date");
        mSource = intent.getStringExtra("source");
        mAuthor = intent.getStringExtra("author");
        mDesc = intent.getStringExtra("description");

        if( mImage == null || mImage.isEmpty()) {
            imageView.setImageResource(R.drawable.icon_news);
        } else {
            Picasso
                    .get()
                    .load(mImage)
                    .fit()
                    .centerCrop()
                    .into(imageView);
        }

        appbar_title.setText(mSource);
        appbar_subTitle.setText(mUrl);
        date.setText(DateTime.DateFormat(mDate));
        title.setText(mTitle);

        String author;
        if (mAuthor != null || mAuthor != "") {
            author = " \u2022 " + mAuthor;
        } else {
            author = "";
        }

        time.setText(mSource + author + " \u2022 " + DateTime.DateToTimeFormat(mDate));
        startWebView(mUrl);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void startWebView(String url) {
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        supportFinishAfterTransition();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {

        int maxScroll = appBarLayout.getTotalScrollRange();
        float percentage = (float) Math.abs(verticalOffset) / (float) maxScroll;

        if (percentage == 1f && hideToolbar) {
            appbarTitle.setVisibility(View.VISIBLE);
            hideToolbar = !hideToolbar;
        }

        else if (percentage < 1f && hideToolbar) {
            appbarTitle.setVisibility(View.GONE);
            hideToolbar = !hideToolbar;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.share) {
            try {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plan");
                i.putExtra(Intent.EXTRA_SUBJECT, mSource);
                String body = mTitle + "\n\n" + mUrl + "\n\n" + "Shared from Puddle app";
                i.putExtra(Intent.EXTRA_TEXT, body);
                startActivity(Intent.createChooser(i, "Share with : "));
            } catch (Exception e) {
                Toast.makeText(this, "Oops.... Sorry \nCannot be shared", Toast.LENGTH_SHORT).show();
            }
        }

        else if (id == R.id.add_to_bookmark) {

            Cursor data = db.getData();
            while (data.moveToNext()) {
                String author = data.getString(1);
                String title = data.getString(2);
                String desc = data.getString(3);
                String url = data.getString(4);
                String urltoimage = data.getString(5);
                String publish = data.getString(6);
                String source = data.getString(7);
                Article article = new Article();
                Source source1 = new Source();
                article.setAuthor(author);
                article.setTitle(title);
                article.setDescription(desc);
                article.setUrl(url);
                article.setUrlToImage(urltoimage);
                article.setPublishedAt(publish);
                source1.setName(source);
                article.setSource(source1);
                articles.add(article);
            }

            boolean isExisted = false;
            for (int i = 0; i < articles.size(); i++) {
                if (articles.get(i).getUrl().equals(mUrl)) {
                    isExisted = true;
                }
            }

            if (!isExisted) {
                boolean insertData = db.addData(mAuthor, mTitle, mDesc, mUrl, mImage, mDate, mSource);
                if (insertData)
                    Toast.makeText(NewsDetailActivity.this, "Article added to bookmarks.", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(NewsDetailActivity.this, "Unable to add article to bookmarks", Toast.LENGTH_SHORT).show();
            }
            else
                Toast.makeText(NewsDetailActivity.this, "Article already exists in bookmarks.", Toast.LENGTH_SHORT).show();
        }

        else if (id == R.id.view_in_web) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(mUrl));
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}