const PRIVACY_URL = "https://app-privacy-center.blogspot.com/2026/08/booknest.html";

const PrivacyPolicyLink = ({ className = "" }: { className?: string }) => (
  <a
    href={PRIVACY_URL}
    target="_blank"
    rel="noopener noreferrer"
    className={`text-xs text-muted-foreground hover:text-primary underline underline-offset-4 transition-colors ${className}`}
  >
    Privacy Policy
  </a>
);

export { PRIVACY_URL };
export default PrivacyPolicyLink;
