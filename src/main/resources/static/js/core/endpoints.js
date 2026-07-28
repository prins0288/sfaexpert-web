/* ============================================================================
 * API endpoint registry — THE single place every API path is defined.
 *
 * Nothing in the app should hard-code a URL string. Import a path from here:
 *     Api.get(API.master.route.list)
 *     Api.post(API.master.route.status(oid), null, { query: { status: 'N' } })
 *
 * Static paths are plain strings; parameterised paths are functions. Change a
 * route in ONE place and every caller follows. Frozen so nothing mutates it.
 * ==========================================================================*/
(function (window) {
  "use strict";

  const API = {
    auth: {
      login: "/api/auth/login",
    },
    me: "/api/me",
    lookups: "/api/lookups",

    menu: {
      get: "/api/menu",
      favorite: (id) => `/api/menu/favorites/${id}`,   // POST to add, DELETE to remove
    },

    theme: {
      get: "/api/theme",
      update: "/api/theme",          // PUT
      reset: "/api/theme/reset",     // POST ?mode=light|dark
    },

    products: {
      list: "/api/products",
      save: "/api/products",
    },

    master: {
      route: {
        base: "/api/master/route",
        list: "/api/master/route",
        save: "/api/master/route",
        saveMultiple: "/api/master/route/save-multiple",
        get: (oid) => `/api/master/route/${oid}`,
        status: (oid) => `/api/master/route/${oid}/status`,
        template: "/api/master/route/template",
        upload: "/api/master/route/upload",
      },
      area: {
        base: "/api/master/area",
        list: "/api/master/area",
        save: "/api/master/area",
        saveMultiple: "/api/master/area/save-multiple",
        get: (oid) => `/api/master/area/${oid}`,
        status: (oid) => `/api/master/area/${oid}/status`,
        template: "/api/master/area/template",
        upload: "/api/master/area/upload",
      },
      routeArea: {
        base: "/api/master/route-area",
        list: "/api/master/route-area",
        save: "/api/master/route-area",
        status: (oid) => `/api/master/route-area/${oid}/status`,
      },
      clientType: {
        base: "/api/master/client-type",
        list: "/api/master/client-type",
        save: "/api/master/client-type",
        saveMultiple: "/api/master/client-type/save-multiple",
        get: (oid) => `/api/master/client-type/${oid}`,
        status: (oid) => `/api/master/client-type/${oid}/status`,
      },
      client: {
        base: "/api/master/client",
        list: "/api/master/client",
        save: "/api/master/client",
        get: (oid) => `/api/master/client/${oid}`,
        status: (oid) => `/api/master/client/${oid}/status`,
        template: "/api/master/client/template",
        upload: "/api/master/client/upload",
      },
      employee: {
        base: "/api/master/employee",
        list: "/api/master/employee",
        save: "/api/master/employee",
        get: (oid) => `/api/master/employee/${oid}`,
        status: (oid) => `/api/master/employee/${oid}/status`,
      },
    },
  };

  function deepFreeze(o) {
    Object.getOwnPropertyNames(o).forEach((k) => {
      const v = o[k];
      if (v && typeof v === "object") deepFreeze(v);
    });
    return Object.freeze(o);
  }

  window.API = deepFreeze(API);
})(window);
