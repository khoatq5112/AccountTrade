1. Fix Price Range Slider

The price range input slider is currently not working.

Ensure that:

The slider updates the selected min and max price values correctly.

The product list filters dynamically based on the selected price range.

The UI reflects the updated values in real time.

2. Fix Navbar Search Input

The search input in the navbar is not functioning.

Implement dynamic search suggestions:

When the user types, the system should fetch and suggest matching accounts dynamically.

Suggestions should appear in a dropdown below the search bar.

Each suggestion should display:

Account name

Possibly avatar or short info (optional)

Clicking a suggestion should navigate to that account’s detail page.

3. Login Check for "Mua ngay"

In the account product cards, the "Mua ngay" (Buy Now) button should:

Check if the buyer is logged in.

If the user is not logged in, redirect them to the login page.

If the user is logged in, proceed to the checkout or purchase flow.

4. Add "Add to Cart" Button

For each product card, add a new button:

"Add to Cart"

When clicked:

The product should be added to the user's cart.

If the user is not logged in, redirect them to the login page.

Optionally display a success notification (e.g., "Product added to cart").