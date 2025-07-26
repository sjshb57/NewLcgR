package top.easelink.lcg.ui.main.recommand.viewmodel;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.ArrayList;
import java.util.List;
import top.easelink.lcg.R;
import top.easelink.lcg.ui.main.articles.view.ArticlesFragment;
import top.easelink.lcg.ui.main.model.TabModel;

public class RecommendViewPagerAdapter extends FragmentStateAdapter {

    private final List<TabModel> tabModels;

    public RecommendViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, Context context) {
        super(fragmentActivity);
        tabModels = new ArrayList<>();
        tabModels.add(new TabModel(context.getString(R.string.tab_title_new_thread), "newthread"));
        tabModels.add(new TabModel(context.getString(R.string.tab_title_tech), "tech"));
        tabModels.add(new TabModel(context.getString(R.string.tab_title_hot), "hot"));
        tabModels.add(new TabModel(context.getString(R.string.tab_title_digest), "digest"));
        tabModels.add(new TabModel(context.getString(R.string.tab_title_help), "help"));
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return ArticlesFragment.newInstance(tabModels.get(position).getUrl(), false);
    }

    @Override
    public int getItemCount() {
        return tabModels.size();
    }

    public String getTabTitle(int position) {
        return tabModels.get(position).getTitle();
    }
}