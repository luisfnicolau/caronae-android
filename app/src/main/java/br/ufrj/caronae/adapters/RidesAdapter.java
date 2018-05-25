package br.ufrj.caronae.adapters;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import br.ufrj.caronae.App;
import br.ufrj.caronae.R;
import br.ufrj.caronae.RoundedTransformation;
import br.ufrj.caronae.Util;
import br.ufrj.caronae.acts.RideDetailAct;
import br.ufrj.caronae.httpapis.CaronaeAPI;
import br.ufrj.caronae.models.User;
import br.ufrj.caronae.models.modelsforjson.MyRidesForJson;
import br.ufrj.caronae.models.modelsforjson.RideForJson;
import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RidesAdapter extends RecyclerView.Adapter<RidesAdapter.ViewHolder> {

    private final int TYPE_HEADER = 0;
    private final int TYPE_BODY = 1;
    private final int TYPE_ZERO = 2;

    private final Context context;
    private List<RideForJson> rideOffers;
    private FragmentManager fm;
    private List<Object> mixedList;

    public RidesAdapter(List<RideForJson> rideOffers, Context context, FragmentManager fm) {
        this.rideOffers = rideOffers;
        this.context = context;
        this.fm = fm;
        this.mixedList = new ArrayList<>();

    }

    @Override
    public RidesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View contactView;

        if (viewType == TYPE_HEADER) {
            contactView = inflater.inflate(R.layout.separator_all_rides, parent, false);
        } else if (viewType == TYPE_BODY) {
            contactView = inflater.inflate(R.layout.item_rides_card, parent, false);
        } else {
            contactView = inflater.inflate(R.layout.separator_all_rides, parent, false);
        }

        return new ViewHolder(contactView);
    }

    @Override
    public int getItemViewType(int position) {
        if (mixedList == null){
            return TYPE_ZERO;
        } else if (mixedList.size() == 0){
            return TYPE_ZERO;
        }
        if (mixedList.get(position).getClass() == Integer.class) {
            return TYPE_HEADER;
        }
        return TYPE_BODY;
    }


    @Override
    public void onBindViewHolder(final RidesAdapter.ViewHolder viewHolder, int position) {
        if (!(mixedList == null || mixedList.size() == 0)) {
            if (mixedList.get(position).getClass().equals(RideForJson.class)) {
                final RideForJson rideOffer = (RideForJson) mixedList.get(position);

                int color = Util.getColors(rideOffer.getZone());
                viewHolder.location_tv.setTextColor(color);
                viewHolder.time_tv.setTextColor(color);
                viewHolder.name_tv.setTextColor(color);
                viewHolder.photo_iv.setBorderColor(color);

                if(rideOffer.type != null && rideOffer.type.equals("final"))
                {
                    viewHolder.secondaryLayout.setVisibility(View.VISIBLE);
                }
                else
                {
                    viewHolder.secondaryLayout.setVisibility(View.GONE);
                }
                String profilePicUrl = rideOffer.getDriver().getProfilePicUrl();
                if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                    Picasso.with(context).load(profilePicUrl)
                            .placeholder(R.drawable.user_pic)
                            .error(R.drawable.user_pic)
                            .transform(new RoundedTransformation())
                            .into(viewHolder.photo_iv);
                } else {
                    viewHolder.photo_iv.setImageResource(R.drawable.user_pic);
                }

                String timeText;
                if (rideOffer.isGoing())
                    timeText = context.getResources().getString(R.string.arrivingAt, Util.formatTime(rideOffer.getTime()));
                else
                    timeText = context.getResources().getString(R.string.leavingAt, Util.formatTime(rideOffer.getTime()));

                timeText =  timeText + " | " + Util.getWeekDayFromDateWithoutTodayString(rideOffer.getDate()) + " | " +Util.formatBadDateWithoutYear(rideOffer.getDate());
                viewHolder.time_tv.setText(timeText);

                String name = rideOffer.getDriver().getName();
                try {
                    String[] split = name.split(" ");
                    String shortName = split[0] + " " + split[split.length - 1];
                    viewHolder.name_tv.setText(shortName);
                } catch (Exception e) {
                    viewHolder.name_tv.setText(name);
                }

                String location;
                if (rideOffer.isGoing())
                    location = rideOffer.getNeighborhood().toUpperCase() + " ➜ " + rideOffer.getHub().toUpperCase();
                else
                    location = rideOffer.getHub().toUpperCase() + " ➜ " + rideOffer.getNeighborhood().toUpperCase();

                viewHolder.location_tv.setText(location);

                boolean requested = false;

                viewHolder.requestIndicator_iv.setVisibility(requested ? View.VISIBLE : View.INVISIBLE);

                viewHolder.parentLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(context, RideDetailAct.class);
                        intent.putExtra("fromWhere", rideOffer.fromWhere);
                        intent.putExtra("ride", rideOffer);
                        intent.putExtra("starting", true);
                        intent.putExtra("rideId", rideOffer.getId().intValue());
                        context.startActivity(intent);
                    }
                });
            }
        }
    }

    public void makeList(List<RideForJson> rideOffers) {
        this.rideOffers = rideOffers;
        List<Integer> headerPositions = getHeaderPositionsOnList(rideOffers);
        mixedList.clear();
        mixedList.addAll(rideOffers);
        if (headerPositions != null && headerPositions.size() > 0) {
            for (int headerCount = 0; headerCount < headerPositions.size(); headerCount++) {
                mixedList.add(headerPositions.get(headerCount) + headerCount, headerPositions.get(headerCount));
            }
        }
        notifyDataSetChanged();
    }

    public void addToList(List<RideForJson> rideOffers) {
        this.rideOffers.addAll(rideOffers);
        notifyDataSetChanged();
    }

    private void verifyRequesters(String idRide, final RidesAdapter.ViewHolder viewHolder)
    {
        CaronaeAPI.service(context).getRequesters(idRide).enqueue(new Callback<List<User>>()
        {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response)
            {
                if (response.isSuccessful())
                {
                    List<User> requesters = response.body();
                    if(requesters != null && !requesters.isEmpty())
                    {
                        //Action that shows to the user that there are requests in this ride.
                    }
                }
                else
                {
                    Util.treatResponseFromServer(response);
                    Log.e("Error ", response.message());
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t)
            {
                Log.e("Error ", t.getLocalizedMessage());
            }
        });
    }

    public void remove(int rideId) {
        for (int i = 0; i < rideOffers.size(); i++)
            if (rideOffers.get(i).getDbId() == rideId) {
                rideOffers.remove(i);
                notifyItemRemoved(i);
            }
    }

    @Override
    public int getItemCount() {
        if (mixedList == null || mixedList.size() == 0) {
            return 1;
        }
        return mixedList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public CircleImageView photo_iv;
        public ImageView requestIndicator_iv;
        public TextView time_tv;
        public TextView location_tv;
        public TextView name_tv;
        public LinearLayout parentLayout;
        public RelativeLayout secondaryLayout;

        public ViewHolder(View itemView) {
            super(itemView);
            photo_iv = itemView.findViewById(R.id.photo_iv);
            requestIndicator_iv = itemView.findViewById(R.id.requestIndicator_iv);
            time_tv = itemView.findViewById(R.id.time_tv);
            location_tv = itemView.findViewById(R.id.location_tv);
            name_tv = itemView.findViewById(R.id.name_tv);
            parentLayout = itemView.findViewById(R.id.cardView);
            secondaryLayout = itemView.findViewById(R.id.secondary_lay);
        }
    }

    private List<Integer> getHeaderPositionsOnList(List<RideForJson> rides) {
        List<Integer> headersPositions = new ArrayList<>();
        if (rides != null) {
            if (rides.size() > 0) {
                headersPositions.add(0);
                for (int rideIndex = 1; rideIndex < rides.size(); rideIndex++) {
                    if (Util.getDayFromDate(rides.get(rideIndex).getDate()) > Util.getDayFromDate(rides.get(rideIndex - 1).getDate())) {
                        headersPositions.add(rideIndex);
                    }
                }
                return headersPositions;
            }
        }
        return null;
    }
}