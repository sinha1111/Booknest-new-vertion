import { useEffect } from "react";

/** Purono /privacy link jeno notun external policy page e niye jay. */
const ExternalRedirect = ({ to }: { to: string }) => {
  useEffect(() => {
    window.location.replace(to);
  }, [to]);
  return null;
};

export default ExternalRedirect;
