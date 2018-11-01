package br.ufrj.caronae.httpapis;

import android.util.Log;

import br.ufrj.caronae.data.SharedPref;
import br.ufrj.caronae.models.User;
import br.ufrj.caronae.models.modelsforjson.LoginForJson;
import br.ufrj.caronae.models.modelsforjson.UserForJson;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginService {
    public void signIn(String id, String token, ServiceCallback<User> callback) {
        SharedPref.saveUserJWTToken(token);

        CaronaeAPI.service().getUser(id).enqueue(new Callback<UserForJson>() {
            @Override
            public void onResponse(Call<UserForJson> call, Response<UserForJson> response) {
                User user;
                try {
                    user = parseUserFromResponse(response);
                } catch (Exception e) {
                    callback.fail(e);
                    return;
                }

                SharedPref.saveUser(user);
                SharedPref.saveNotifPref("true");
                callback.success(user);
            }

            @Override
            public void onFailure(Call<UserForJson> call, Throwable t) {
                callback.fail(t);
            }
        });
    }

    public void signInLegacy(String idUFRJ, String token, ServiceCallback<User> callback) {
        CaronaeAPI.service().login(new LoginForJson(token, idUFRJ)).enqueue(new Callback<UserForJson>() {
            @Override
            public void onResponse(Call<UserForJson> call, Response<UserForJson> response) {
                User user;
                try {
                    user = parseUserFromResponse(response);
                } catch (Exception e) {
                    callback.fail(e);
                    return;
                }

                SharedPref.saveUser(user);
                SharedPref.saveUserToken(token);
                SharedPref.saveUserIdUfrj(idUFRJ);
                SharedPref.saveNotifPref("true");
                callback.success(user);
            }

            @Override
            public void onFailure(Call<UserForJson> call, Throwable t) {
                callback.fail(t);
            }
        });
    }

    private User parseUserFromResponse(Response<UserForJson> response) throws Exception {
        if (!response.isSuccessful()) {
            if (response.code() == 401) {
                Log.e("Login", "Failure: login denied");
                throw new InvalidCredentialsException();
            }

            Log.e("Login", "Failure: login request was not successful. " + response.message());
            throw new Exception("Login request was not successful" + response.message());
        }

        UserForJson userJson = response.body();
        if (userJson == null || userJson.getUser() == null) {
            Log.e("Login", "Failure: user was not found in response");
            throw new Exception("User not found in response");
        }

        return userJson.getUser();
    }
}
