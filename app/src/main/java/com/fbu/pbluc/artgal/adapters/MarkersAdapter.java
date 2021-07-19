package com.fbu.pbluc.artgal.adapters;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fbu.pbluc.artgal.R;
import com.fbu.pbluc.artgal.models.Marker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class MarkersAdapter extends RecyclerView.Adapter<MarkersAdapter.ViewHolder> {

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
    View view = LayoutInflater.from(mContext).inflate(R.layout.item_marker, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull MarkersAdapter.ViewHolder holder, int position) {
    Marker marker = mMarkers.get(position);
    holder.bind(marker);
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

  public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvAugmentedObjectFileName;
    private TextView tvCreatedAt;

    public ViewHolder(@NonNull View itemView) {
      super(itemView);

      tvTitle = itemView.findViewById(R.id.tvTitle);
      tvDescription = itemView.findViewById(R.id.tvDescription);
      tvAugmentedObjectFileName = itemView.findViewById(R.id.tvAugmentedObjectFileName);
      tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);

      itemView.setOnClickListener(this);
    }

    public void bind(Marker marker) {
      tvTitle.setText(marker.getTitle());
      tvDescription.setText(marker.getDescription());
      tvAugmentedObjectFileName.setText(marker.getAugmentedObj().get(Marker.KEY_FILENAME).toString().substring(49));
      tvCreatedAt.setText("Created " + marker.calculateTimeAgo());
    }

    @Override
    public void onClick(View view) {
      int position = getAdapterPosition();
      switch (view.getId()) {
        default:
          mOnClickListener.onListItemClick(position);
          break;
      }
    }
  }

  public interface ListItemClickListener {
    void onListItemClick(int position);
  }
}
