package com.fbu.pbluc.artgal.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fbu.pbluc.artgal.R;
import com.fbu.pbluc.artgal.models.Marker;

import java.util.List;

public class MarkersAdapter extends RecyclerView.Adapter<MarkersAdapter.ViewHolder> {

  private static final String TAG = "MarkersAdapter";

  // Two view types which will be used to determine whether a row should be displaying
  // data or a Progressbar
  public static final int VIEW_TYPE_LOADING = 0;
  public static final int VIEW_TYPE_ITEM = 1;
  private List<Marker> mMarkers;
  private Context mContext;

  final private ListItemClickListener mOnClickListener;

  public MarkersAdapter(List<Marker> mMarkers, Context mContext, ListItemClickListener onClickListener) {
    this.mMarkers = mMarkers;
    this.mContext = mContext;
    this.mOnClickListener = onClickListener;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = null;
    if (viewType == VIEW_TYPE_ITEM) {
      view = LayoutInflater.from(mContext).inflate(R.layout.item_marker, parent, false);
      return new DataViewHolder(view);
    } else {
      view = LayoutInflater.from(mContext).inflate(R.layout.item_progress, parent, false);
      return new ProgressViewHolder(view);
    }
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    if (holder instanceof DataViewHolder) {
      Marker marker = mMarkers.get(position);
      ((DataViewHolder) holder).bind(marker);
    }
  }

  @Override
  public int getItemViewType(int position) {
    if (mMarkers.get(position) != null) {
      return VIEW_TYPE_ITEM;
    }
    return VIEW_TYPE_LOADING;
  }

  @Override
  public int getItemCount() {
    return mMarkers.size();
  }

  // Clean all elements of the recycler
  public void clear() {
    mMarkers.clear();
    notifyDataSetChanged();
  }

  // Add a list of items
  public void addAll(List<Marker> markers) {
    mMarkers.addAll(markers);
    notifyDataSetChanged();
  }

  public void addNullData() {
    mMarkers.add(null);
    notifyItemInserted(mMarkers.size() - 1);
  }

  public void removeNull() {
    mMarkers.remove(mMarkers.size() - 1);
    notifyItemRemoved(mMarkers.size());
  }

  class DataViewHolder extends ViewHolder implements View.OnClickListener, View.OnLongClickListener {
    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvAugmentedObjectFileName;
    private TextView tvCreatedAt;
    private TextView tvLikeCount;
    private ImageView ivLikeMarker;

    public DataViewHolder(@NonNull View itemView) {
      super(itemView);

      tvTitle = itemView.findViewById(R.id.tvTitle);
      tvDescription = itemView.findViewById(R.id.tvDescription);
      tvAugmentedObjectFileName = itemView.findViewById(R.id.tvAugmentedObjectFileName);
      tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
      tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
      ivLikeMarker = itemView.findViewById(R.id.ivLikeMarker);

      ivLikeMarker.setOnClickListener(this);
      itemView.setOnClickListener(this);
      itemView.setOnLongClickListener(this);
    }

    public void bind(Marker marker) {
      tvTitle.setText(marker.getTitle());
      tvDescription.setText(marker.getDescription());
      tvAugmentedObjectFileName.setText(marker.getAugmentedObj().get(Marker.KEY_FILENAME).toString().substring(49));
      tvCreatedAt.setText("Created " + marker.calculateTimeAgo());

      if (marker.getLikedCount() == null || marker.getLikedCount() == 0) {
        tvLikeCount.setVisibility(View.INVISIBLE);
      } else {
        tvLikeCount.setVisibility(View.VISIBLE);
        tvLikeCount.setText(Integer.toString(marker.getLikedCount()));
      }
    }

    @Override
    public void onClick(View view) {
      int position = getAdapterPosition();
      switch (view.getId()) {
        case R.id.ivLikeMarker:
          mOnClickListener.onLikeClick(position, view);
          break;
        default:
          mOnClickListener.onListItemClick(position);
          break;
      }
    }

    @Override
    public boolean onLongClick(View view) {
      int position = getAdapterPosition();
      switch (view.getId()) {
        default:
          mOnClickListener.onListItemLongClick(position, view);
          break;
      }
      return true;
    }
  }

  class ProgressViewHolder extends ViewHolder {
    public ProgressViewHolder(View itemView) {
      super(itemView);
    }
  }

  public class ViewHolder extends RecyclerView.ViewHolder {
    public ViewHolder(@NonNull View itemView) {
      super(itemView);
    }
  }

  public interface ListItemClickListener {
    void onListItemClick(int position);
    void onListItemLongClick(int position, View view);
    void onLikeClick(int position, View view);
  }

}
