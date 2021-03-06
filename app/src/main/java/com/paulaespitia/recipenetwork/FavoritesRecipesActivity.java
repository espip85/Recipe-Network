package com.paulaespitia.recipenetwork;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TextView;

import com.paulaespitia.recipenetwork.model.RecipeID;
import com.paulaespitia.recipenetwork.model.SQLAsyncTask;
import com.paulaespitia.recipenetwork.model.SQLHelper;
import com.paulaespitia.recipenetwork.model.User;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FavoritesRecipesActivity extends MenuActivity {

    TableLayout favoriteRecipesTable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites_recipes);

        favoriteRecipesTable = findViewById(R.id.favoriteRecipesTable);

        GetFavoritesTask getFavoritesTask = new GetFavoritesTask(this, favoriteRecipesTable);
        getFavoritesTask.execute();
    }

    public void removeRecipe(View view) {
        View tableRow = (View) view.getParent();
        favoriteRecipesTable.removeView(tableRow);
        Integer idRecipes = (Integer) tableRow.getTag();

        RemoveFavoriteTask removeFavoriteTask = new RemoveFavoriteTask(this, favoriteRecipesTable);
        removeFavoriteTask.execute(idRecipes);
    }

    private static class RemoveFavoriteTask extends SQLAsyncTask<Integer, Void> {

        final WeakReference<TableLayout> weakFavoriteRecipesTable;

        public RemoveFavoriteTask(Activity activity, TableLayout favoriteRecipesTable) {
            super(activity);
            weakFavoriteRecipesTable = new WeakReference<>(favoriteRecipesTable);
        }

        @Override
        protected Void sqlBackground(Connection connection, Integer... integers) throws SQLException {
            PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM FavoriteRecipes WHERE idUsers=? AND idRecipes=?");
            preparedStatement.setInt(1, User.currentUser.userId);
            preparedStatement.setInt(2, integers[0]);   // recipeID
            preparedStatement.executeUpdate();
            return null;
        }

        @Override
        protected void sqlPostExecute(Activity activity, Void aVoid) {

        }
    }

    private static class GetFavoritesTask extends SQLAsyncTask<Void, List<RecipeID>> {

        final WeakReference<TableLayout> weakFavoriteRecipesTable;

        public GetFavoritesTask(Activity activity, TableLayout favoriteRecipesTable) {
            super(activity);
            weakFavoriteRecipesTable = new WeakReference<>(favoriteRecipesTable);
        }

        @Override
        protected List<RecipeID> sqlBackground(Connection connection, Void... voids) throws SQLException {
            List<RecipeID> favorites = new LinkedList<>();
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT FavoriteRecipes.idRecipes,Recipes.name,Users.username FROM FavoriteRecipes,Recipes,Users WHERE FavoriteRecipes.idUsers=? AND Recipes.idRecipes=FavoriteRecipes.idRecipes AND Users.idUsers=Recipes.idUsers;");
            preparedStatement.setInt(1, User.currentUser.userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                favorites.add(new RecipeID(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3)));
            }
            return favorites;
        }

        @Override
        protected void sqlPostExecute(Activity activity, List<RecipeID> recipeIDs) {
            if (weakFavoriteRecipesTable.get() != null) {
                LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                for (RecipeID favorite : recipeIDs) {
                    final View rowView = inflater.inflate(R.layout.favorites_recipes_recipe, null);
                    rowView.setTag(favorite.recipeID);
                    TextView nameTextView = rowView.findViewById(R.id.favoritesRecipesRecipeName);
                    nameTextView.setText(favorite.name);
                    TextView authorTextView = rowView.findViewById(R.id.favoritesRecipesRecipeAuthor);
                    authorTextView.setText(String.format("%s %s", activity.getResources().getString(R.string.created_by), favorite.author));
                    weakFavoriteRecipesTable.get().addView(rowView);
                }
            }
        }
    }
}
