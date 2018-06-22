/***
 * This is an example JS
 * This comment will be deleted on minification :(
 * I'm NOT a JS dev, so I don't understand most of this. It's just for testing.
 */
(function () {
  $.extend(true, window, {
    "NodePlugin": {
      "Test": Test
    }
  });

  /***
   * Tests methods
   * @param name Your name
   * @constructor
   */
  function Test(name) {
    this.name = name;

    /**
     * Alert with your name
     */
    this.sayMyName = function () {
      alert("Your name is " + this.name);
    };

    /**
     * Calculate the factorial of a number
     * @param n {Number} A number
     * @returns {number} n!
     */
    this.factorial = function (n) {
      var result = 1;
      for (var i = 2; i < n; i++) {
        result *= i;
      }
      return result;
    };

    /**
     * Calculate the fibonacci series
     * @param n {Number} A number
     * @returns {Number} The n element of the fibonacci series
     */
    this.fibonacci = function (n) {
      return this.fibonacci(n - 1) + this.fibonacci(n - 2);
    }
  }
})(jQuery);