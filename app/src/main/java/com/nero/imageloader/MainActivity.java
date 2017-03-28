package com.nero.imageloader;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

public class MainActivity extends Activity{

	RecyclerView recyclerView;

	private ImageLoader mImageLoader;
	private LayoutInflater mInflater;
	private Drawable mDefaultBitmapDrawable;

	private List<String> mUrList = new ArrayList<String>();

	private boolean mIsRecyclerViewIdle = true;
	private int mImageWidth = 0;
	private boolean mIsWifi = false;
	private boolean mCanGetBitmapFromNetWork = false;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mImageLoader = ImageLoader.build(this);
		mInflater = LayoutInflater.from(this);
		mDefaultBitmapDrawable = this.getResources().getDrawable(R.drawable.image_default);

		initData();
		initView();
	}

	private void initData() {
		String[] imageUrls = {
				"http://pic47.nipic.com/20140830/7487939_180041822000_2.jpg",
				"http://b.hiphotos.baidu.com/zhidao/pic/item/a6efce1b9d16fdfafee0cfb5b68f8c5495ee7bd8.jpg",
				"http://pic41.nipic.com/20140518/4135003_102912523000_2.jpg",
				"http://img2.imgtn.bdimg.com/it/u=1133260524,1171054226&fm=21&gp=0.jpg",
				"http://h.hiphotos.baidu.com/image/pic/item/3b87e950352ac65c0f1f6e9efff2b21192138ac0.jpg",
				"http://pic42.nipic.com/20140618/9448607_210533564001_2.jpg",
				"http://pic10.nipic.com/20101027/3578782_201643041706_2.jpg",
				"http://img2.3lian.com/2014/c7/51/d/26.jpg",
				"http://img3.3lian.com/2013/c1/34/d/93.jpg",
				"http://b.zol-img.com.cn/desk/bizhi/image/3/960x600/1375841395686.jpg",
				"http://cdn.duitang.com/uploads/item/201311/03/20131103171224_rr2aL.jpeg",
				"http://imgrt.pconline.com.cn/images/upload/upc/tx/wallpaper/1210/17/c1/spcgroup/14468225_1350443478079_1680x1050.jpg",
				"http://pic41.nipic.com/20140518/4135003_102025858000_2.jpg",
				"http://www.1tong.com/uploads/wallpaper/landscapes/200-4-730x456.jpg",
				"http://pic.58pic.com/58pic/13/00/22/32M58PICV6U.jpg",
				"http://h.hiphotos.baidu.com/zhidao/wh%3D450%2C600/sign=429e7b1b92ef76c6d087f32fa826d1cc/7acb0a46f21fbe09cc206a2e69600c338744ad8a.jpg",
				"http://pica.nipic.com/2007-12-21/2007122115114908_2.jpg",
				"http://cdn.duitang.com/uploads/item/201405/13/20140513212305_XcKLG.jpeg",
				"http://photo.loveyd.com/uploads/allimg/080618/1110324.jpg",
				"http://img4.duitang.com/uploads/item/201404/17/20140417105820_GuEHe.thumb.700_0.jpeg",
				"http://cdn.duitang.com/uploads/item/201204/21/20120421155228_i52eX.thumb.600_0.jpeg",
				"http://img4.duitang.com/uploads/item/201404/17/20140417105856_LTayu.thumb.700_0.jpeg",
				"http://img04.tooopen.com/images/20130723/tooopen_20530699.jpg",
				"http://pic.dbw.cn/0/01/33/59/1335968_847719.jpg",
				"http://a.hiphotos.baidu.com/image/pic/item/a8773912b31bb051a862339c337adab44bede0c4.jpg",
				"http://h.hiphotos.baidu.com/image/pic/item/f11f3a292df5e0feeea8a30f5e6034a85edf720f.jpg",
				"http://img0.pconline.com.cn/pconline/bizi/desktop/1412/ER2.jpg",
				"http://pic.58pic.com/58pic/11/25/04/91v58PIC6Xy.jpg",
				"http://img3.3lian.com/2013/c2/32/d/101.jpg",
				"http://pic25.nipic.com/20121210/7447430_172514301000_2.jpg",
		};
		for (String url : imageUrls) {
			mUrList.add(url);
		}
		int screenWidth = MyUtils.getScreenMetrics(this).widthPixels;
		int space = (int)MyUtils.dp2px(this, 20f);
		mImageWidth = (screenWidth - space) / 3;
		mIsWifi = MyUtils.isWifi(this);
		if (mIsWifi) {
			mCanGetBitmapFromNetWork = true;
		}
	}

	private void initView(){
		recyclerView = (RecyclerView)findViewById(R.id.recycleView);
		recyclerView.setLayoutManager(new GridLayoutManager(this,3));
		recyclerView.addItemDecoration(new SpacesItemDecoration((int)MyUtils.dp2px(this, 5f)));
		recyclerView.setAdapter(mImageAdapter);
		recyclerView.addOnScrollListener(new OnScrollListener(){
			@Override
			public void onScrollStateChanged(final RecyclerView recyclerView,final int newState){
				super.onScrollStateChanged(recyclerView,newState);
				if(newState == SCROLL_STATE_IDLE){
					mIsRecyclerViewIdle = true;
					mImageAdapter.notifyDataSetChanged();
				}else{
					mIsRecyclerViewIdle = false;
				}
			}
		});
	}

	private RecyclerView.Adapter<ViewHolder> mImageAdapter = new RecyclerView.Adapter<ViewHolder>(){

		@Override
		public ViewHolder onCreateViewHolder(final ViewGroup parent,final int viewType){
			mInflater = LayoutInflater.from(parent.getContext());
			View itemView = mInflater.inflate(R.layout.image_item,parent,false);
			return new ViewHolder(itemView);
		}

		@Override
		public void onBindViewHolder(final ViewHolder holder,final int position){
			ImageView imageView = holder.imageView;
			final String tag = (String)imageView.getTag();
			final String uri = mUrList.get(position);
			if(!uri.equals(tag)){
				imageView.setImageDrawable(mDefaultBitmapDrawable);
			}
			if(mIsRecyclerViewIdle && mCanGetBitmapFromNetWork){
				imageView.setTag(uri);
				mImageLoader.bindBitmap(uri,imageView,mImageWidth,mImageWidth);
			}
		}

		@Override
		public int getItemCount(){
			return mUrList.size();
		}
	};

	private static class ViewHolder extends RecyclerView.ViewHolder{

		ImageView imageView;

		public ViewHolder(final View itemView){
			super(itemView);
			imageView = (ImageView)itemView.findViewById(R.id.squareImageView);
		}
	}

	public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
		private int space;

		public SpacesItemDecoration(int space) {
			this.space = space;
		}

		@Override
		public void getItemOffsets(Rect outRect,View view,
		                           RecyclerView parent,RecyclerView.State state) {
			//outRect.left = space;
			outRect.right = space;
			outRect.bottom = space;

			// Add top margin only for the first item to avoid double space between items
			int childPosition = parent.getChildLayoutPosition(view);
			if (childPosition == 0 || childPosition == 1 || childPosition == 2) {
				outRect.top = space;
			} else {
				outRect.top = 0;
			}
		}
	}
}
