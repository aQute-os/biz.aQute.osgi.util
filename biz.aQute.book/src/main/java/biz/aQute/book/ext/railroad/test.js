
version(200);

  function* enumerate(iter) {
    var count = 0;
    for (var x of iter) {
      yield [count, x];
      count++;
    }
  }
